package org.sandbox.chat.http

import org.sandbox.chat.ChatServer
import org.sandbox.chat.Settings
import org.sandbox.chat.sse.SseChatPublisher
import org.sandbox.chat.sse.SseChatService
import org.sandbox.chat.sse.SseChatServiceActions

import akka.actor.ActorRef
import akka.actor.ActorSystem
import akka.actor.Props

object HttpChatApp extends App {

  implicit val system = ActorSystem("chat-http")
  val settings = Settings(system)

  val chatPublisher: ActorRef = system.actorOf(Props[SseChatPublisher])
  val sseChatService =
    system.actorOf(SseChatService.props(
        settings.sseService.interface, settings.sseService.port,
        chatPublisher))
  ServiceActor.getStatus(sseChatService)

  val chatServer = system.actorOf(ChatServer.props(chatPublisher), "ChuckNorris")

  val chatServiceActions = //new HttpChatServerActions(chatServer, system)
    new SseChatServiceActions(chatServer, chatPublisher, system)

  val httpChatService =
    system.actorOf(HttpChatService.props(
        settings.httpService.interface, settings.httpService.port,
        chatServer, chatServiceActions, chatPublisher))
  ServiceActor.getStatus(httpChatService)

  println(s"HttpChatApp with ActorSystem ${system.name} started")
  system.registerOnTermination(println(s"ActorSystem ${system.name} shutting down ..."))

  system.awaitTermination
}
