package org.sandbox.chat.http

import org.sandbox.chat.ChatServer.Ack
import org.sandbox.chat.ChatServer.Ackable
import org.sandbox.chat.ChatServer.Contribution
import org.sandbox.chat.ChatServer.Join
import org.sandbox.chat.ChatServer.Leave

import HttpChatClient.Broadcasts
import akka.actor.ActorRef
import akka.actor.ActorSystem
import akka.http.model.HttpEntity.apply
import akka.http.model.HttpResponse
import akka.http.model.StatusCodes.InternalServerError
import akka.http.model.StatusCodes.NotFound
import akka.http.model.StatusCodes.OK

class HttpChatServiceActionsImpl(val chatServer: ActorRef, val system: ActorSystem)
  extends HttpChatServiceActions[HttpResponse] with Participants[HttpResponse]
{
  import org.sandbox.chat.ChatServer._

  private def ok(msg: String) = HttpResponse(OK, entity = s"$msg\n")
  override def notFound(name: String) = HttpResponse(NotFound, entity = s"not found: $name\n")

  private def withAck(who: ActorRef, msg: Ackable)(onAck: => HttpResponse) = {
    val Ack(ackedMsg) = askFor[Ack](who, msg)
    if (msg == ackedMsg) onAck
    else HttpResponse(InternalServerError, entity = s"unexpected Ack for $msg")
  }

  override def onJoin(name: String) = {
    val participant = createParticipant(name)
    withAck(participant.who, Join(participant)) {
      addParticipant(participant)
      ok(s"joined: $name")
    }
  }

  override def onLeave(name: String) = {
    forParticipant(name) { participant =>
      withAck(participant.who, Leave(participant)) {
        removeParticipant(participant)
        ok(s"left: $name")
      }
    }
  }
  override def onContribution(name: String, msg: String) = {
    forParticipant(name) { participant =>
      withAck(participant.who, Contribution(participant, msg)) {
        ok(s"contribution: $msg")
      }
    }
  }
  override def onPoll(name: String) = {
    forParticipant(name) { participant =>
      val Broadcasts(messages) = askForBroadcasts(participant)
      ok(s"${messages.mkString("\n")}")
    }
  }
  override def onShutdown = {
    val participantNames = askForShutdown
    ok(s"shutdown: ${system.name} (participants: ${participantNames.mkString(", ")})")
  }
}
