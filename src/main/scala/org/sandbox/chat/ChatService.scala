package org.sandbox.chat

import scala.concurrent.duration.DurationInt

import ChatServer.Ack
import ChatServer.Broadcast
import ChatServer.ChatServerMsg
import ChatServer.Contribution
import ChatServer.Participant
import ChatServer.ParticipantAdminMsg
import ChatServer.Shutdown
import ParticipantAdministrator.AllParticipants
import ParticipantAdministrator.GetAllParticipants
import ParticipantAdministrator.IsJoined
import akka.actor.Actor
import akka.actor.ActorLogging
import akka.actor.ActorRef
import akka.actor.actorRef2Scala
import akka.event.LoggingReceive
import akka.pattern.ask
import akka.pattern.pipe
import akka.util.Timeout

trait ChatService extends Actor with ServiceActor with ActorLogging {
  import ChatServer._
  import ParticipantAdministrator._
  import context.dispatcher

  val settings = Settings(context.system)

  // collaborators
  def chatMsgPublisher: ActorRef
  def participantAdmin: ActorRef
  def broadcastManager: ActorRef

  val shutdownSystem: Boolean

  private def publish(msg: ChatServerMsg) =
    chatMsgPublisher ! msg

  implicit val timeout = Timeout(1 second)

  private def chatReceive: Receive = {
    case msg: ParticipantAdminMsg =>
      val ack = (participantAdmin ? msg).mapTo[Ack]
      ack.onSuccess { case Ack(msg) => publish(msg) }
      ack pipeTo sender
    case contribution@Contribution(author@Participant(_,name), msg) =>
      val requestor = sender
      val isJoined = (participantAdmin ? IsJoined(author)).mapTo[IsJoined]
      isJoined onSuccess {
        case IsJoined(_, isJoined) if isJoined =>
          requestor ! Ack(contribution)
          self ! Broadcast(name, msg)
          publish(contribution)
      }
    case broadcast: Broadcast =>
      broadcastManager ! broadcast
      publish(broadcast)
    case shutdown: Shutdown =>
      val requestor = sender
      val allParticipants = (participantAdmin ? GetAllParticipants).mapTo[AllParticipants]
      allParticipants onSuccess {
        case AllParticipants(participants) =>
          val msg = shutdown.copy(participants = participants)
          publish(msg)
          requestor ! Ack(msg)
          if (shutdownSystem) {
            log.info(s"ChatServer ${self.path.name} to shutdown in ${500 millis} ...")
            context.system.scheduler.scheduleOnce(500 millis)(context.system.shutdown)
          } else {
            context.stop(self)
          }
      }
  }

  def chatServiceReceive: Receive = LoggingReceive {
    chatReceive orElse serviceReceive
  }

  override def postStop = {
    log.info(s"ChatService ${self.path.name} shutting down ...")
    super.postStop
  }

  log.info(s"ChatService ${self.path.name} started")
}
