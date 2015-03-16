package org.sandbox.chat.http

import akka.stream.scaladsl.Source
import de.heikoseeberger.akkasse.EventStreamMarshalling
import de.heikoseeberger.akkasse.ServerSentEvent
import akka.http.server.Route
import akka.actor.ActorRef
import akka.actor.ActorSystem
import akka.http.marshalling.ToResponseMarshallable
import SseChatServerActions._

class SseChatServerActions(val chatServer: ActorRef, val system: ActorSystem)
  extends ChatServerActions[ToResponseMarshallable] with Participants[ToResponseMarshallable]
  with EventStreamMarshalling
{
  import org.sandbox.chat.ChatServer._
  import system.dispatcher

  override def notFound(name: String): ToResponseMarshallable =
    singleSource(ServerSentEvent(name, "notfound"))

  private def singleSource[T](element: T) = Source.single(element)

  private def tellWithAckReceiver(who: ActorRef, msg: Ackable, onAckReceived: => Unit = ()) = {
    def createAckReceiver: ActorRef = {
      def onAck(ack: Ack) = ack match {
        case Ack(ackedMsg) if ackedMsg == msg =>
          onAckReceived
          singleSource(chatServerMsgToServerSentEvent(ack))
      }
      system.actorOf(AckReceiver.props(onAck))
    }

    val ackReceiver = createAckReceiver
    who tell (msg, ackReceiver)
  }

  def onContribution(name: String, msg: String): ToResponseMarshallable = {
    forParticipant(name) { participant =>
      val contribution = Contribution(participant, msg)
      tellWithAckReceiver(participant.who, contribution)
      singleSource(chatServerMsgToServerSentEvent(contribution))
    }
  }

  def onJoin(name: String): ToResponseMarshallable = {
    val participant = createParticipant(name)
    val msg = Join(participant)
    tellWithAckReceiver(participant.who, msg, addParticipant(participant))
    singleSource(chatServerMsgToServerSentEvent(msg))
  }

  def onLeave(name: String): ToResponseMarshallable = {
    ???
  }

  def onPoll(name: String): ToResponseMarshallable = {
    ???
  }

  def onShutdown: ToResponseMarshallable = {
    ???
  }
}

object SseChatServerActions {
  import org.sandbox.chat.ChatServer._
  implicit def chatServerMsgToServerSentEvent(message: ChatServerMsg): ServerSentEvent = {
    val event = message.getClass.getSimpleName.toLowerCase
    message match {
      case m => ServerSentEvent(m.toString, event)
    }
  }
}