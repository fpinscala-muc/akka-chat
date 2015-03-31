package org.sandbox.chat

import java.util.Date

import org.sandbox.chat.http.HttpChatService
import org.sandbox.chat.http.HttpChatServiceActionsImpl
import org.sandbox.chat.sse.SseChatService

import akka.actor.ActorRef
import akka.actor.Props

class ChatServer(override val shutdownSystem: Boolean) extends ChatService {
  import ChatServer._

  override val chatMsgPublisher = context.actorOf(Props[ChatMsgPublisher])
  override val participantAdmin = context.actorOf(ParticipantAdministrator.props())
  override val broadcastManager = context.actorOf(BroadcastManager.props(participantAdmin))

  val httpChatService = createHttpService
  val sseChatService = createSseService

  def receive = chatServiceReceive

  private def createHttpService: ActorRef = {
    val chatServiceActions = new HttpChatServiceActionsImpl(self, context.system)
    val httpChatService =
      context.actorOf(HttpChatService.props(chatServiceActions))
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
