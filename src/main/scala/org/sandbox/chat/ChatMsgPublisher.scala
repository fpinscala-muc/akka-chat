package org.sandbox.chat

class ChatMsgPublisher extends ChatMsgPublishing {
  def receive: Receive = chatMsgReceive
}

object ChatMsgPublisher {
  sealed trait ChatMsgPublisherMsg
  case object ChatMsgAccepted extends ChatMsgPublisherMsg
  case object ChatMsgDenied extends ChatMsgPublisherMsg
}
