package com.waz.zclient.messages

import com.waz.api.Message

sealed trait MsgPart
sealed trait SeparatorPart extends MsgPart

object MsgPart {
  case object Separator extends SeparatorPart
  case object SeparatorLarge extends SeparatorPart
  case object User extends MsgPart
  case object Text extends MsgPart
  case object Ping extends MsgPart
  case object Rename extends MsgPart
  case object FileAsset extends MsgPart
  case object AudioAsset extends MsgPart
  case object VideoAsset extends MsgPart
  case object Image extends MsgPart
  case object WebLink extends MsgPart
  case object YouTube extends MsgPart
  case object Location extends MsgPart
  case object SoundCloud extends MsgPart
  case object MemberChange extends MsgPart
  case object ConnectRequest extends MsgPart
  case object Footer extends MsgPart
  case object InviteBanner extends MsgPart
  case object OtrMessage extends MsgPart
  case object MissedCall extends MsgPart
  case object EphemeralDots extends MsgPart
  case object Empty extends MsgPart
  case object Unknown extends MsgPart

  def apply(msgType: Message.Type): MsgPart = {
    import Message.Type._
    msgType match {
      case TEXT | TEXT_EMOJI_ONLY => Text
      case ASSET => Image
      case ANY_ASSET => FileAsset
      case VIDEO_ASSET => VideoAsset
      case AUDIO_ASSET => AudioAsset
      case LOCATION => Location
      case MEMBER_JOIN | MEMBER_LEAVE => MemberChange
      case CONNECT_REQUEST => ConnectRequest
      case OTR_ERROR | OTR_DEVICE_ADDED | OTR_IDENTITY_CHANGED | OTR_UNVERIFIED | OTR_VERIFIED | HISTORY_LOST | STARTED_USING_DEVICE => OtrMessage
      case KNOCK => Ping
      case RENAME => Rename
      case MISSED_CALL => MissedCall
      case RECALLED => Empty // recalled messages only have an icon in header
      case CONNECT_ACCEPTED | INCOMING_CALL => Empty // those are never used in messages (only in notifications)
      case RICH_MEDIA => Empty // RICH_MEDIA will be handled separately
      case UNKNOWN => Unknown
    }
  }

  def apply(msgType: Message.Part.Type): MsgPart = {
    import Message.Part.Type._
    msgType match {
      case TEXT | TEXT_EMOJI_ONLY => Text
      case ASSET => Image
      case WEB_LINK => WebLink
      case ANY_ASSET => FileAsset
      case SOUNDCLOUD => SoundCloud
      case YOUTUBE => YouTube
      case GOOGLE_MAPS | SPOTIFY | TWITTER => Text
    }
  }
}
