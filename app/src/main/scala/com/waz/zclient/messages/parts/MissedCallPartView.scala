package com.waz.zclient.messages.parts

import android.content.Context
import android.util.AttributeSet
import android.widget.RelativeLayout
import com.waz.ZLog.ImplicitTag._
import com.waz.model._
import com.waz.service.ZMessaging
import com.waz.threading.Threading
import com.waz.utils._
import com.waz.utils.events.Signal
import com.waz.zclient.common.views.ChatheadView
import com.waz.zclient.controllers.global.AccentColorController
import com.waz.zclient.messages.{MessageViewPart, MsgPart}
import com.waz.zclient.ui.text.TypefaceTextView
import com.waz.zclient.ui.utils.TextViewUtils
import com.waz.zclient.utils.ContextUtils._
import com.waz.zclient.views.calling.StaticCallingIndicator
import com.waz.zclient.{R, ViewHelper}
import org.threeten.bp.Instant

import scala.concurrent.duration._

class MissedCallPartView(context: Context, attrs: AttributeSet, style: Int) extends RelativeLayout(context, attrs, style) with MessageViewPart with ViewHelper {
  def this(context: Context, attrs: AttributeSet) = this(context, attrs, 0)

  def this(context: Context) = this(context, null, 0)

  override val tpe: MsgPart = MsgPart.MissedCall

  inflate(R.layout.message_missed_call_content)

  private val chathead: ChatheadView = findById(R.id.chathead)
  private val tvMessage: TypefaceTextView = findById(R.id.tvMessage)
  private val callIndicator: StaticCallingIndicator = findById(R.id.callIndicator)

  private val zms = inject[Signal[ZMessaging]]
  private val accent = inject[AccentColorController]
  private val userId = Signal[UserId]()

  private val user = Signal(zms, userId).flatMap {
    case (z, id) => z.usersStorage.signal(id)
  }

  private val locale = context.getResources.getConfiguration.locale
  private val msg = user map {
    case u if u.isSelf => getString(R.string.content__missed_call__you_called)
    case u if u.getDisplayName.isEmpty => ""
    case u =>
      getString(R.string.content__missed_call__xxx_called, u.getDisplayName.toUpperCase(locale))
  }

  userId(chathead.setUserId)

  msg.on(Threading.Ui) { m =>
    tvMessage.setText(m)
    TextViewUtils.boldText(tvMessage)
  }

  accent.accentColor.on(Threading.Ui) { callIndicator.setColor }

  override def set(pos: Int, msg: MessageData, part: Option[MessageContent], widthHint: Int): Unit = {
    userId ! msg.userId

    // start animation if this is the first time this message is shown
    // how do we properly detect that?
    // what if user scrolls back, maybe we should continue previous animation
    if (msg.localTime >= Instant.now - 5.seconds) {
      callIndicator.runAnimation()
    }
  }
}
