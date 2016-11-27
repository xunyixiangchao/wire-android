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

import java.util

import android.support.v7.widget.RecyclerView
import android.view.ViewGroup
import com.waz.ZLog.ImplicitTag._
import com.waz.ZLog._
import com.waz.service.ZMessaging
import com.waz.threading.Threading
import com.waz.utils.events.{EventContext, EventStream, Signal}
import com.waz.zclient.controllers.global.SelectionController
import com.waz.zclient.messages.ItemChangeAnimator.ChangeInfo
import com.waz.zclient.messages.MessageView.MsgOptions
import com.waz.zclient.{Injectable, Injector}

class MessagesListAdapter(listWidth: Signal[Int])(implicit inj: Injector, ec: EventContext) extends RecyclerView.Adapter[MessageViewHolder]() with Injectable with MessagesListView.Adapter { adapter =>

  verbose("MessagesListAdapter created")

  val zms = inject[Signal[ZMessaging]]
  override val selectedConversation = inject[SelectionController].selectedConv

  val onBindView = EventStream[Int]()
  val showUnreadDot = Signal[Boolean](false)
  override val nextUnreadIndex = Signal[Int]()

  val cursor = zms.zip(selectedConversation) map { case (zs, conv) => new RecyclerCursor(conv, zs, adapter) }
  override val msgCount = cursor.flatMap(_.countSignal)

  private var messages = Option.empty[RecyclerCursor]

  cursor.on(Threading.Ui) { c =>
    messages.foreach(_.close())
    verbose(s"cursor changed: ${c.count}")
    messages = Some(c)
    notifyDataSetChanged()
  }

  override def getItemCount: Int = messages.fold(0)(_.count)

  def message(position: Int) = messages.get.apply(position)

  def currentLastReadIndex() = messages.fold(-1)(_.lastReadIndex())

  override def getItemViewType(position: Int): Int = MessageView.viewType(message(position).message.msgType)

  override def onBindViewHolder(holder: MessageViewHolder, pos: Int): Unit = {
    onBindViewHolder(holder, pos, new util.ArrayList[AnyRef])
  }

  override def onBindViewHolder(holder: MessageViewHolder, pos: Int, payloads: util.List[AnyRef]): Unit = {
    verbose(s"onBindViewHolder: position: $pos")
    val data = message(pos)
    val isSelf = zms.currentValue.exists(_.selfUserId == data.message.userId)
    val isFirstUnread = pos > 0 && !isSelf && showUnreadAtPos.currentValue.exists { case (show, p) => show && p == pos }
    val opts = MsgOptions(pos, getItemCount, isSelf, isFirstUnread, listWidth.currentValue.getOrElse(0))

    holder.bind(data, if (pos == 0) None else Some(message(pos - 1).message), opts, changeInfo(payloads))
    onBindView ! pos
  }

  private def changeInfo(payloads: util.List[AnyRef]) =
    if (payloads.size() != 1) None // we only handle single partial change, will default to full restart otherwise
    else payloads.get(0) match {
      case ci: ChangeInfo => Some(ci)
      case _ => None
    }

  val showUnreadAtPos = showUnreadDot.zip(nextUnreadIndex)
  showUnreadAtPos.onChanged.on(Threading.Ui) { case (_, pos) => notifyItemChanged(pos) }

  override def onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder =
    MessageViewHolder(MessageView(parent, viewType), adapter)

  // view depends on two message entries,
  // most importantly, view needs to be refreshed if previous message was added or removed
  // TODO: test if that actually works
  registerAdapterDataObserver(new RecyclerView.AdapterDataObserver {

    private def notifyChangedIfExists(position: Int) =
      if (position >= 0 && position < getItemCount) notifyItemChanged(position)

    override def onItemRangeRemoved(positionStart: Int, itemCount: Int): Unit =
      notifyChangedIfExists(positionStart)

    override def onItemRangeMoved(fromPosition: Int, toPosition: Int, itemCount: Int): Unit =
      if (fromPosition < toPosition) {
        notifyChangedIfExists(fromPosition)
        notifyChangedIfExists(toPosition + itemCount)
      } else {
        notifyChangedIfExists(fromPosition + 1)
        notifyChangedIfExists(toPosition + itemCount + 1)
      }

    override def onItemRangeInserted(positionStart: Int, itemCount: Int): Unit =
      notifyChangedIfExists(positionStart + itemCount + 1)
  })
}
