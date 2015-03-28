package org.sandbox.chat

import java.util.Date

import scala.concurrent.duration.DurationInt

import org.sandbox.chat.http.HttpChatService
import org.sandbox.chat.http.HttpChatServiceActionsImpl
import org.sandbox.chat.sse.SseChatService

import ParticipantAdministrator.AllParticipants
import ParticipantAdministrator.GetAllParticipants
import ParticipantAdministrator.IsJoined
import akka.actor.Actor
import akka.actor.ActorLogging
import akka.actor.ActorRef
import akka.actor.Props
import akka.actor.actorRef2Scala
import akka.event.LoggingReceive
import akka.pattern.ask
import akka.pattern.pipe
import akka.util.Timeout

class ChatServer(shutdownSystem: Boolean) extends Actor with ServiceActor with ActorLogging {
  import ChatServer._
  import ParticipantAdministrator._
  import context.dispatcher

  val settings = Settings(context.system)

  val chatMsgPublisher = context.actorOf(Props[ChatMsgPublisher])
  val participantAdmin = context.actorOf(ParticipantAdministrator.props())
  val broadcastManager = context.actorOf(BroadcastManager.props(participantAdmin))

  val httpChatService = createHttpService
  val sseChatService = createSseService

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

  override def receive: Receive = LoggingReceive {
    chatReceive orElse serviceReceive
  }

  override def postStop = {
    log.info(s"ChatServer ${self.path.name} shutting down ...")
    super.postStop
  }

  log.info(s"ChatServer ${self.path.name} started")

  private def createHttpService: ActorRef = {
    val chatServiceActions = new HttpChatServiceActionsImpl(self, context.system)
    val httpChatService =
      context.actorOf(HttpChatService.props(
        settings.httpService.interface, settings.httpService.port,
        self, chatServiceActions))
    waitForRunningService(httpChatService)
  }

  private def createSseService: ActorRef = {
    val sseChatService =
      context.actorOf(SseChatService.props(
        settings.sseService.interface, settings.sseService.port,
        chatMsgPublisher))
    waitForRunningService(sseChatService)
  }
}

object ChatServer {
  def props(shutdownSystem: Boolean): Props =
    Props(new ChatServer(shutdownSystem))

  case class Participant(who: ActorRef, name: String)

  trait ParticipantAdminMsg

  sealed trait ChatServerMsg
  trait Ackable extends ChatServerMsg
  case class Join(who: Participant) extends ChatServerMsg with Ackable with ParticipantAdminMsg
  case class Leave(who: Participant) extends ChatServerMsg with Ackable with ParticipantAdminMsg
  case class Contribution(author: Participant, msg: String) extends ChatServerMsg with Ackable
  case class Broadcast(authorName: String, msg: String, when: Date  = new Date) extends ChatServerMsg
  case class Shutdown(participants: Set[Participant] = Set()) extends ChatServerMsg with Ackable
  case class Ack(what: Ackable) extends ChatServerMsg

  private def waitForRunningService(service: ActorRef) = {
    val status = ServiceActor.getStatus(service)
    require(status == ServiceActor.StatusRunning)
    service
  }
}