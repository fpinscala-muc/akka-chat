package org.sandbox.chat.http

import org.sandbox.chat.ChatServer
import akka.actor.ActorRef
import akka.actor.ActorSystem
import akka.actor.Props
import akka.http.Http
import akka.http.server.Route
import akka.stream.ActorFlowMaterializer
import akka.stream.scaladsl.Sink
import akka.stream.scaladsl.Source
import org.sandbox.chat.sse.SseChatPublisher
import de.heikoseeberger.akkasse.ServerSentEvent
import org.sandbox.chat.sse.SseChatService
import akka.stream.ActorFlowMaterializerSettings
import akka.stream.actor.ActorPublisher

object HttpChatApp extends App {

  implicit val system = ActorSystem("chat-http")
  import system.dispatcher
  implicit val materializer = ActorFlowMaterializer()

  val chatServer = system.actorOf(Props[ChatServer], "ChuckNorris")

  val sseChatPublisher: ActorRef = system.actorOf(Props[SseChatPublisher])
  implicit val sseSource: Source[ServerSentEvent, _] =
    Source(ActorPublisher[ServerSentEvent](sseChatPublisher))
  val sseChatService = new SseChatService(sseSource, system, materializer)
  val chatServerActions = //new HttpChatServerActions(chatServer, system)
    new SseChatServerActions(chatServer, sseChatPublisher, system)
  val chatRoutes = ChatRoutes(chatServerActions)


  val requestHandler = Route.handlerFlow(chatRoutes)
  val serverSource = Http(system).bind(interface = "localhost", port = 8080)
//  Http(system).bindAndstartHandlingWith(requestHandler, interface = "localhost", port = 8080)

  val bindingFuture = serverSource.to(Sink.foreach { connection =>
    println("Accepted new connection from " + connection.remoteAddress)
    connection handleWith requestHandler
  }).run()
}
