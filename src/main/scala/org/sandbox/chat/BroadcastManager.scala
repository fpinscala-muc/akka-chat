package org.sandbox.chat

import scala.concurrent.duration.DurationInt
import akka.actor.Actor
import akka.actor.ActorRef
import akka.actor.Props
import akka.pattern.{ ask, pipe }
import akka.util.Timeout

class BroadcastManager(participantAdmin: ActorRef) extends Actor {
  import ParticipantAdministrator._
  import ChatServer._

  import context.dispatcher
  implicit val timeout = Timeout(1 second)

  def receive: Receive = {
    case broadcast: Broadcast =>
      val allParticipants = (participantAdmin ? GetAllParticipants).mapTo[AllParticipants]
      allParticipants onSuccess {
        case AllParticipants(participants) => participants foreach(_.who ! broadcast)
      }
  }
}

object BroadcastManager {
  def props(participantAdmin: ActorRef): Props =
    Props(new BroadcastManager(participantAdmin))
}
