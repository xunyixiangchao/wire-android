/**
 * Wire
 * Copyright (C) 2016 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.waz.zclient.messages

import android.content.Context
import android.util.AttributeSet
import android.view.{HapticFeedbackConstants, ViewGroup}
import android.widget.LinearLayout
import com.waz.ZLog.ImplicitTag._
import com.waz.ZLog._
import com.waz.api.Message
import com.waz.model.ConversationData.ConversationType
import com.waz.model.{MessageContent, MessageData, MessageId}
import com.waz.service.messages.MessageAndLikes
import com.waz.utils.RichOption
import com.waz.zclient.controllers.global.SelectionController
import com.waz.zclient.messages.MsgPart._
import com.waz.zclient.messages.controllers.MessageActionsController
import com.waz.zclient.utils.ContextUtils._
import com.waz.zclient.utils.DateConvertUtils.asZonedDateTime
import com.waz.zclient.utils._
import com.waz.zclient.{BuildConfig, R, ViewHelper}
import org.threeten.bp.Instant

class MessageView(context: Context, attrs: AttributeSet, style: Int) extends LinearLayout(context, attrs, style) with ViewHelper {

  import MessageView._

  def this(context: Context, attrs: AttributeSet) = this(context, attrs, 0)
  def this(context: Context) = this(context, null, 0)

  private val factory = inject[MessageViewFactory]
  private val selection = inject[SelectionController].messages
  private lazy val messageActions = inject[MessageActionsController]

  private var msgId: MessageId = _
  private var msg: MessageData = MessageData.Empty
  private var data: MessageAndLikes = MessageAndLikes.Empty

  private var footer = Option.empty[Footer]

  this.onClick {
    selection.toggleFocused(msgId)
  }

  this.onLongClick {
    performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
    messageActions.showDialog(data)
  }

  private var pos = -1 //messages position for debugging only

  def set(mAndL: MessageAndLikes, prev: Option[MessageData], opts: MsgOptions): Unit = {
    data = mAndL
    msg = mAndL.message
    pos = opts.position
    msgId = msg.id
    verbose(s"set $pos, $mAndL")

    val contentParts = {
      if (msg.msgType != Message.Type.RICH_MEDIA) Seq(MsgPart(msg.msgType) -> None)
      else msg.content map { content => MsgPart(content.tpe) -> Some(content) }
    } .filter(_ != MsgPart.Empty)

    val parts =
      if (!BuildConfig.DEBUG && contentParts.forall(_._1 == MsgPart.Unknown)) Nil // don't display anything for unknown message
      else {
        val builder = Seq.newBuilder[(MsgPart, Option[MessageContent])]

        getSeparatorType(msg, prev, opts.isFirstUnread).foreach(sep => builder += sep -> None)

        if (shouldShowChathead(msg, prev))
          builder += MsgPart.User -> None

        if (shouldShowInviteBanner(msg, opts)) {
          builder += MsgPart.InviteBanner -> None
        }
        builder ++= contentParts

        if (focusableTypes.contains(msg.msgType))//|| mAndL.likes.nonEmpty) TODO need to trigger open animation if liked
          builder += MsgPart.Footer -> None

        builder.result()
      }

    if (parts.nonEmpty) this.setMarginTop(getTopMargin(prev.map(_.msgType), parts.head._1))
    setParts(mAndL, parts, opts)
  }

  def getFooter: Option[Footer] = footer

  private def getSeparatorType(msg: MessageData, prev: Option[MessageData], isFirstUnread: Boolean): Option[MsgPart] = msg.msgType match {
    case Message.Type.CONNECT_REQUEST => None
    case _ =>
      prev.fold2(None, { p =>
        val prevDay = asZonedDateTime(p.time).toLocalDate.atStartOfDay()
        val curDay = asZonedDateTime(msg.time).toLocalDate.atStartOfDay()

        if (prevDay.isBefore(curDay)) Some(SeparatorLarge)
        else if (p.time.isBefore(msg.time.minusSeconds(1800)) || isFirstUnread) Some(Separator)
        else None
      })
  }

  private def shouldShowChathead(msg: MessageData, prev: Option[MessageData]) = {
    val userChanged = prev.forall(m => m.userId != msg.userId || m.isSystemMessage)
    val recalled = msg.msgType == Message.Type.RECALLED
    val edited = msg.editTime != Instant.EPOCH
    val knock = msg.msgType == Message.Type.KNOCK

    !knock && !msg.isSystemMessage && (recalled || edited || userChanged)
  }

  private def shouldShowInviteBanner(msg: MessageData, opts: MsgOptions) =
    opts.position == 0 && msg.msgType == Message.Type.MEMBER_JOIN && opts.convType == ConversationType.Group

  private def setParts(msg: MessageAndLikes, parts: Seq[(MsgPart, Option[MessageContent])], opts: MsgOptions) = {
    verbose(s"setParts: opts: $opts, parts: ${parts.map(_._1)}")

    // recycle views in reverse order, recycled views are stored in a Stack, this way we will get the same views back if parts are the same
    // XXX: once views get bigger, we may need to optimise this, we don't need to remove views that will get reused, currently this seems to be fast enough
    (0 until getChildCount).reverseIterator.map(getChildAt) foreach {
      case pv: MessageViewPart => factory.recycle(pv)
      case _ =>
    }
    removeAllViewsInLayout()

    parts.zipWithIndex foreach { case ((tpe, content), index) =>
      val view = factory.get(tpe, this)
      view.set(msg, content, opts)
      view match {
        case v: Footer => footer = Some(v)
        case _ =>
      }
      addViewInLayout(view, index, Option(view.getLayoutParams) getOrElse factory.DefaultLayoutParams)
    }
  }

}

object MessageView {

  val focusableTypes = Set(
    Message.Type.TEXT,
    Message.Type.TEXT_EMOJI_ONLY,
    Message.Type.ANY_ASSET,
    Message.Type.ASSET,
    Message.Type.AUDIO_ASSET,
    Message.Type.VIDEO_ASSET,
    Message.Type.LOCATION,
    Message.Type.RICH_MEDIA
  )

  val GenericMessage = 0

  def viewType(tpe: Message.Type): Int = tpe match {
    case _ => GenericMessage
  }

  def apply(parent: ViewGroup, tpe: Int): MessageView = tpe match {
    case _ => ViewHelper.inflate[MessageView](R.layout.message_view, parent, addToParent = false)
  }

  trait MarginRule

  case object TextLike extends MarginRule
  case object ImageLike extends MarginRule
  case object FileLike extends MarginRule
  case object MemberChange extends MarginRule
  case object Rename extends MarginRule
  case object Ping extends MarginRule
  case object MissedCall extends MarginRule
  case object Other extends MarginRule

  object MarginRule {
    def apply(tpe: Message.Type): MarginRule = apply(MsgPart(tpe))

    def apply(tpe: MsgPart): MarginRule = {
      tpe match {
        case Separator |
             SeparatorLarge |
             User |
             Text => TextLike
        case MsgPart.Ping => Ping
        case MsgPart.Rename => Rename
        case FileAsset |
             AudioAsset |
             WebLink |
             YouTube |
             Location |
             SoundCloud => FileLike
        case Image | VideoAsset => ImageLike
        case MsgPart.MemberChange => MemberChange
        case _ => Other
      }
    }
  }

  def getTopMargin(prevTpe: Option[Message.Type], topPart: MsgPart)(implicit context: Context): Int = {
    if (prevTpe.isEmpty)
      if (MarginRule(topPart) == MemberChange) toPx(24) else 0
    else {
      val p = (MarginRule(prevTpe.get), MarginRule(topPart)) match {
        case (TextLike, TextLike)         => 8
        case (TextLike, FileLike)         => 16
        case (FileLike, FileLike)         => 10
        case (ImageLike, ImageLike)       => 0
        case (FileLike | ImageLike, _) |
             (_, FileLike | ImageLike)    => 10
        case (Rename, _)                  => 24
        case (MissedCall, _)              => 24
        case (MemberChange, _) |
             (_, MemberChange)            => 24
        case (_, Ping) | (Ping, _)        => 14
        case (_, MissedCall)              => 24
        case _                            => 0
      }
      toPx(p)
    }
  }

  case class MsgOptions(
                         position: Int,
                         totalCount: Int,
                         isSelf: Boolean,
                         isFirstUnread: Boolean,
                         widthHint: Int,
                         convType: ConversationType
                       ) {
    def isLast: Boolean = position == totalCount - 1
  }
}
