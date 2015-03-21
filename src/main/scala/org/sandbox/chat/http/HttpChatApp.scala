package org.sandbox.chat.http

import org.sandbox.chat.ChatServer
import org.sandbox.chat.Settings
import org.sandbox.chat.sse.SseChatService

import akka.actor.ActorRef
import akka.actor.ActorSystem
import akka.http.Http
import akka.http.server.Route
import akka.stream.ActorFlowMaterializer
import akka.stream.scaladsl.Sink

object HttpChatApp extends App {

  implicit val system = ActorSystem("chat-http")
  val settings = Settings(system)
  import system.dispatcher
  implicit val materializer = ActorFlowMaterializer()

  val sseChatService = new SseChatService

  val chatPublisher: ActorRef = sseChatService.getPublisher
  val chatServer = system.actorOf(ChatServer.props(chatPublisher), "ChuckNorris")

  val chatServerActions = //new HttpChatServerActions(chatServer, system)
    sseChatService.getChatServerActions(chatServer)

  val chatRoutes = ChatRoutes(chatServerActions)

  val interface = settings.httpService.interface
  val port = settings.httpService.port

  val requestHandler = Route.handlerFlow(chatRoutes)
  val serverSource = Http(system).bind(interface = interface, port = port)
//  Http(system).bindAndstartHandlingWith(requestHandler, interface = "localhost", port = 8080)

  val bindingFuture = serverSource.to(Sink.foreach { connection =>
    system.log.info(s"HttpChatApp: accepted new connection from ${connection.remoteAddress}")
    connection handleWith requestHandler
  }).run()

  println(s"HttpChatApp listening on $interface:$port")
  system.registerOnTermination(println(s"ActorSystem ${system.name} shutting down ..."))
}
