package org.sandbox.chat.http

import org.sandbox.chat.ChatServer
import org.sandbox.chat.sse.SseChatPublisher
import org.sandbox.chat.sse.SseChatServerActions
import org.sandbox.chat.sse.SseChatService
import akka.actor.ActorRef
import akka.actor.ActorSystem
import akka.actor.Props
import akka.http.Http
import akka.http.server.Route
import akka.stream.ActorFlowMaterializer
import akka.stream.actor.ActorPublisher
import akka.stream.scaladsl.Sink
import akka.stream.scaladsl.Source
import de.heikoseeberger.akkasse.ServerSentEvent

object HttpChatApp extends App {

  implicit val system = ActorSystem("chat-http")
  import system.dispatcher
  implicit val materializer = ActorFlowMaterializer()

  val sseChatPublisher: ActorRef = system.actorOf(Props[SseChatPublisher])

  val chatServer = system.actorOf(ChatServer.props(sseChatPublisher), "ChuckNorris")

  implicit val sseSource: Source[ServerSentEvent, _] =
    Source(ActorPublisher[ServerSentEvent](sseChatPublisher))
  val sseChatService = new SseChatService(sseSource, system, materializer)
  val chatServerActions = //new HttpChatServerActions(chatServer, system)
    new SseChatServerActions(chatServer, sseChatPublisher, system)
  val chatRoutes = ChatRoutes(chatServerActions)

  val host = "localhost"
  val port = 8080

  val requestHandler = Route.handlerFlow(chatRoutes)
  val serverSource = Http(system).bind(interface = host, port = port)
//  Http(system).bindAndstartHandlingWith(requestHandler, interface = "localhost", port = 8080)

  val bindingFuture = serverSource.to(Sink.foreach { connection =>
    system.log.info(s"HttpChatApp: accepted new connection from ${connection.remoteAddress}")
    connection handleWith requestHandler
  }).run()

  println(s"HttpChatApp listening on $host:$port")
  system.registerOnTermination(println(s"ActorSystem ${system.name} shutting down ..."))
}
