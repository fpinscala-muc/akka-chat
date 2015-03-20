package org.sandbox.chat.sse

import scala.annotation.tailrec

import org.sandbox.chat.ChatServer.ChatServerMsg

import akka.actor.actorRef2Scala
import akka.stream.actor.ActorPublisher
import akka.stream.actor.ActorPublisherMessage.Cancel
import akka.stream.actor.ActorPublisherMessage.Request
import de.heikoseeberger.akkasse.ServerSentEvent

object SseChatPublisher {
  sealed trait SseChatPublisherMsg
  case object SseAccepted
  case object SseDenied
}

class SseChatPublisher extends ActorPublisher[ServerSentEvent] {
  import SseChatPublisher._
  import akka.stream.actor.ActorPublisherMessage._

  val MaxBufferSize = 100
  var buf = Vector.empty[ServerSentEvent]

  def receive: Receive =
    publishSse orElse convertChatServerMsg

  private def publishSse: Receive = {
    case sse: ServerSentEvent if buf.size == MaxBufferSize =>
      sender ! SseDenied
    case sse: ServerSentEvent =>
      sender ! SseAccepted
      if (buf.isEmpty && totalDemand > 0)
        onNext(sse)
      else {
        buf :+= sse
        deliverBuf()
      }
    case Request(_) =>
      deliverBuf()
    case Cancel =>
      context.stop(self)
  }

  private def convertChatServerMsg: Receive = {
    case msg: ChatServerMsg => self ! SseConversions.chatServerMsgToSse(msg)
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