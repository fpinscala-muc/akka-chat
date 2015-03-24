package org.sandbox.chat

import scala.annotation.tailrec

import org.sandbox.chat.ChatServer.ChatServerMsg

import akka.actor.actorRef2Scala
import akka.stream.actor.ActorPublisher
import akka.stream.actor.ActorPublisherMessage.Cancel
import akka.stream.actor.ActorPublisherMessage.Request

class ChatMsgPublisher extends ActorPublisher[ChatServerMsg] {
  import ChatMsgPublisher._

  val MaxBufferSize = 100
  var buf = Vector.empty[ChatServerMsg]

  def receive: Receive = {
    case msg: ChatServerMsg if buf.size == MaxBufferSize =>
      sender ! ChatMsgDenied
    case msg: ChatServerMsg =>
      sender ! ChatMsgAccepted
      if (buf.isEmpty && totalDemand > 0)
        onNext(msg)
      else {
        buf :+= msg
        deliverBuf()
      }
    case Request(_) =>
      deliverBuf()
    case Cancel =>
      context.stop(self)
  }

  @tailrec final def deliverBuf(): Unit =
    if (totalDemand > 0) {
      // totalDemand is a Long and could be larger than what buf.splitAt can accept
      if (totalDemand <= Int.MaxValue) {
        val (use, keep) = buf.splitAt(totalDemand.toInt)
        buf = keep
        use foreach onNext
      } else {
        val (use, keep) = buf.splitAt(Int.MaxValue)
        buf = keep
        use foreach onNext
        deliverBuf()
      }
    }
}

object ChatMsgPublisher {
  sealed trait ChatMsgPublisherMsg
  case object ChatMsgAccepted extends ChatMsgPublisherMsg
  case object ChatMsgDenied extends ChatMsgPublisherMsg
}
