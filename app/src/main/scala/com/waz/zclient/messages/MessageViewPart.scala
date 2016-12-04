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
import android.graphics.{Canvas, Paint}
import android.text.format.DateFormat
import android.util.AttributeSet
import android.view.View
import android.widget.{LinearLayout, RelativeLayout}
import com.waz.ZLog.ImplicitTag._
import com.waz.api.Message
import com.waz.model._
import com.waz.service.ZMessaging
import com.waz.service.messages.MessageAndLikes
import com.waz.threading.Threading
import com.waz.utils.events.Signal
import com.waz.zclient.common.views.ChatheadView
import com.waz.zclient.controllers.global.AccentColorController
import com.waz.zclient.messages.MessageView.MsgOptions
import com.waz.zclient.messages.MsgPart.Footer
import com.waz.zclient.messages.parts.EphemeralDotsDrawable
import com.waz.zclient.ui.text.{GlyphTextView, TypefaceTextView}
import com.waz.zclient.ui.theme.ThemeUtils
import com.waz.zclient.utils.ContextUtils.{getColor, getDimenPx}
import com.waz.zclient.utils.ZTimeFormatter.getSeparatorTime
import com.waz.zclient.utils._
import com.waz.zclient.{R, ViewHelper}
import org.threeten.bp.{Instant, LocalDateTime, ZoneId}


trait MessageViewPart extends View {
  val tpe: MsgPart

  def set(msg: MessageData, part: Option[MessageContent], opts: MsgOptions): Unit

  def set(msg: MessageAndLikes, part: Option[MessageContent], opts: MsgOptions): Unit =
    set(msg.message, part, opts)
}

// Marker for view parts that should be laid out as in FrameLayout (instead of LinearLayout)
trait FrameLayoutPart extends MessageViewPart

trait Footer extends MessageViewPart {
  override val tpe = Footer

  //for animation
  def setContentTranslationY(translation: Float): Unit

  def getContentTranslation: Float

  def updateLikes(likedBySelf: Boolean, likes: IndexedSeq[UserId]): Unit
}

trait TimeSeparator extends MessageViewPart with ViewHelper {

  val is24HourFormat = DateFormat.is24HourFormat(getContext)

  lazy val timeText: TypefaceTextView = findById(R.id.separator__time)
  lazy val unreadDot: UnreadDot = findById(R.id.unread_dot)

  val time = Signal[Instant]()
  val text = time map { t =>
    getSeparatorTime(getContext.getResources, LocalDateTime.now, DateConvertUtils.asLocalDateTime(t), is24HourFormat, ZoneId.systemDefault, true)
  }

  text.on(Threading.Ui)(timeText.setTransformedText)

  def set(msg: MessageData, part: Option[MessageContent], opts: MsgOptions): Unit = {
    this.time ! msg.time
    unreadDot.show ! opts.isFirstUnread
  }

  this.onClick {} //confusing if message opens when timestamp clicked
}

class SeparatorView(context: Context, attrs: AttributeSet, style: Int) extends RelativeLayout(context, attrs, style) with TimeSeparator {
  def this(context: Context, attrs: AttributeSet) = this(context, attrs, 0)
  def this(context: Context) = this(context, null, 0)

  override val tpe: MsgPart = MsgPart.Separator
}

class SeparatorViewLarge(context: Context, attrs: AttributeSet, style: Int) extends LinearLayout(context, attrs, style) with TimeSeparator {
  def this(context: Context, attrs: AttributeSet) = this(context, attrs, 0)
  def this(context: Context) = this(context, null, 0)

  override val tpe: MsgPart = MsgPart.SeparatorLarge

  if (ThemeUtils.isDarkTheme(context)) setBackgroundColor(getColor(R.color.white_8))
  else setBackgroundColor(getColor(R.color.black_4))

}

class UnreadDot(context: Context, attrs: AttributeSet, style: Int) extends View(context, attrs, style) with ViewHelper {
  def this(context: Context, attrs: AttributeSet) = this(context, attrs, 0)
  def this(context: Context) = this(context, null, 0)

  val accent = inject[AccentColorController].accentColor
  val show = Signal[Boolean](false)

  val dotRadius = getDimenPx(R.dimen.conversation__unread_dot__radius)
  val dotPaint = new Paint(Paint.ANTI_ALIAS_FLAG)

  accent { color =>
    dotPaint.setColor(color.getColor())
    invalidate()
  }

  show.onChanged.on(Threading.Ui)(_ => invalidate())

  override def onDraw(canvas: Canvas): Unit = if (show.currentValue.getOrElse(false)) canvas.drawCircle(getWidth / 2, getHeight / 2, dotRadius, dotPaint)
}

class UserPartView(context: Context, attrs: AttributeSet, style: Int) extends LinearLayout(context, attrs, style) with MessageViewPart with ViewHelper {
  def this(context: Context, attrs: AttributeSet) = this(context, attrs, 0)

  def this(context: Context) = this(context, null, 0)

  override val tpe: MsgPart = MsgPart.User

  inflate(R.layout.message_user_content)

  private val chathead: ChatheadView = findById(R.id.chathead)
  private val tvName: TypefaceTextView = findById(R.id.tvName)
  private val tvStateGlyph: GlyphTextView = findById(R.id.gtvStateGlyph)

  private val zms = inject[Signal[ZMessaging]]
  private val userId = Signal[UserId]()
  private val message = Signal[MessageData]

  private val user = Signal(zms, userId).flatMap {
    case (z, id) => z.usersStorage.signal(id)
  }

  private val stateGlyph = message map {
    case m if m.msgType == Message.Type.RECALLED => Some(R.string.glyph__trash)
    case m if m.editTime != Instant.EPOCH => Some(R.string.glyph__edit)
    case _ => None
  }

  userId(chathead.setUserId)

  user.map(_.getDisplayName).on(Threading.Ui)(tvName.setTransformedText)

  stateGlyph.map(_.isDefined) { tvStateGlyph.setVisible }

  stateGlyph.collect { case Some(glyph) => glyph } { tvStateGlyph.setText }

  override def set(msg: MessageData, part: Option[MessageContent], opts: MsgOptions): Unit = {
    userId ! msg.userId
    message ! msg
  }
}

class EmptyPartView(context: Context, attrs: AttributeSet, style: Int) extends View(context, attrs, style) with MessageViewPart {
  def this(context: Context, attrs: AttributeSet) = this(context, attrs, 0)
  def this(context: Context) = this(context, null, 0)

  override val tpe = MsgPart.Empty

  override def set(msg: MessageData, part: Option[MessageContent], opts: MsgOptions): Unit = ()
}

class EphemeralDotsView(context: Context, attrs: AttributeSet, style: Int) extends View(context, attrs, style) with ViewHelper with FrameLayoutPart {
  def this(context: Context, attrs: AttributeSet) = this(context, attrs, 0)
  def this(context: Context) = this(context, null, 0)

  override val tpe = MsgPart.EphemeralDots
  val background = new EphemeralDotsDrawable()

  setBackground(background)

  override def set(msg: MessageData, part: Option[MessageContent], opts: MsgOptions): Unit =
    background.setMessage(msg.id)
}
