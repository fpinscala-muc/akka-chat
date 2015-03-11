package org.sandbox.chat.http

import org.sandbox.chat.ChatServer

import akka.actor.ActorRef
import akka.actor.ActorSystem
import akka.actor.Props
import akka.http.Http
import akka.http.server.Route
import akka.stream.ActorFlowMaterializer
import akka.stream.scaladsl.Sink

object HttpChatApp extends App {

  implicit val system = ActorSystem("chat-http")
  import system.dispatcher

  val chatServer = system.actorOf(Props[ChatServer], "ChuckNorris")

  val chatServerActions = new HttpChatServerActions(chatServer, system)
  import chatServerActions._
  val chatRoutes = ChatRoutes(onJoin, onLeave, onBroadcast, onPoll, onShutdown)

  implicit val materializer = ActorFlowMaterializer()

  val requestHandler = Route.handlerFlow(chatRoutes)
  val serverSource = Http(system).bind(interface = "localhost", port = 8080)
//  Http(system).bindAndstartHandlingWith(requestHandler, interface = "localhost", port = 8080)

  val bindingFuture = serverSource.to(Sink.foreach { connection =>
    println("Accepted new connection from " + connection.remoteAddress)
    connection handleWith requestHandler
  }).run()
}
