package com.waz.zclient.messages.parts

import android.content.Context
import android.graphics._
import android.graphics.drawable.Drawable
import com.waz.model.MessageId
import com.waz.service.ZMessaging
import com.waz.threading.Threading
import com.waz.utils.events.{ClockSignal, EventContext, Signal}
import com.waz.zclient.controllers.global.AccentColorController
import com.waz.zclient.ui.utils.ColorUtils
import com.waz.zclient.utils.ContextUtils._
import com.waz.zclient.{Injectable, Injector, R}

import scala.concurrent.duration._

class EphemeralDotsDrawable(implicit injector: Injector, ctx: Context, ec: EventContext) extends Drawable with Injectable {
  import EphemeralDotsDrawable._

  private val zms = inject[Signal[ZMessaging]]
  private val accent = inject[AccentColorController]

  private val msgId = Signal[MessageId]()

  private val dotPadding = getDimenPx(R.dimen.ephemeral__animating_dots__space_between)

  val msg = for {
    z <- zms
    id <- msgId
    m <- z.messagesStorage.signal(id)
  } yield m

  val selectedDots = msg
    .map { m => (m.ephemeral, m.expired, m.expiryTime) }  // optimisation to ignore unrelated changes
    .flatMap {
      case (ephemeral, expired, expiryTime) =>
        if (expired) Signal const 0
        else expiryTime.fold(Signal const DotsCount) { time =>
          ClockSignal(1.second) map { now =>
            // XXX: clock should be aligned with expiryTime, currently view refresh is not in sync with actual counting
            val remaining = time.toEpochMilli - now.toEpochMilli
            (remaining * (DotsCount + 1) / ephemeral.milliseconds).toInt max 0 min DotsCount
          }
        }
    }

  val state = for {
    color <- accent.accentColor.map(_.getColor())
    secondary = ColorUtils.injectAlpha(getResourceFloat(R.dimen.ephemeral__accent__timer_alpha), color)
    ephemeral <- msg.map(_.isEphemeral)
    dots <- selectedDots
  } yield {
    (ephemeral, dots, color, secondary)
  }

  state.on(Threading.Ui) { _ => invalidateSelf() }

  private var dotHeight: Int = 0
  private val paint = new Paint(Paint.ANTI_ALIAS_FLAG)

  override def setColorFilter(colorFilter: ColorFilter): Unit = paint.setColorFilter(colorFilter)

  override def setAlpha(alpha: Int): Unit = paint.setAlpha(alpha)

  override def getOpacity: Int = PixelFormat.TRANSLUCENT

  override def draw(canvas: Canvas): Unit = state.currentValue match {
    case Some((true, selectedCount, primaryColor, secondaryColor)) if canvas != null && canvas.getHeight > 0 =>
      val x = canvas.getWidth / 2
      val initialY = dotHeight / 2
      val radius = dotHeight.toFloat / 2
      for (i <- 0 until DotsCount) {
        paint.setColor(if (i + selectedCount >= DotsCount) primaryColor else secondaryColor)
        val y = dotHeight * i + initialY + dotPadding * i
        canvas.drawCircle(x, y, radius, paint)
      }
    case _ => // nothing to draw, not ephemeral or not loaded
  }

  override def onBoundsChange(bounds: Rect): Unit = {
    val totalDotPadding = dotPadding * (DotsCount - 1)
    dotHeight = (bounds.height() - totalDotPadding) / DotsCount
    invalidateSelf()
  }

  def setMessage(id: MessageId): Unit = msgId ! id
}

object EphemeralDotsDrawable {
  val DotsCount = 5
  val PrimaryColor = Color.GRAY
  val SecondaryColor = Color.LTGRAY
}
