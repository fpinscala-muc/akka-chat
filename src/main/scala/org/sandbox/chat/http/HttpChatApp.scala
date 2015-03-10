package org.sandbox.chat.http

import scala.concurrent.duration.DurationInt

import org.sandbox.chat.ChatClient
import org.sandbox.chat.ChatServer
import org.sandbox.chat.ChatServer.Broadcast
import org.sandbox.chat.ChatServer.Leave

import akka.actor.ActorRef
import akka.actor.ActorSystem
import akka.actor.Props
import akka.actor.actorRef2Scala
import akka.http.Http
import akka.http.model.HttpEntity.apply
import akka.http.model.HttpResponse
import akka.http.model.StatusCodes.NotFound
import akka.http.model.StatusCodes.OK
import akka.http.server.Route
import akka.stream.ActorFlowMaterializer
import akka.stream.scaladsl.Sink

object HttpChatApp extends App {

  implicit val system = ActorSystem("chat-http")
  import system.dispatcher

  val chatServer = system.actorOf(Props[ChatServer], "ChuckNorris")

  var chatClients: Map[String,ActorRef] = Map.empty

  def onJoin(name: String) = {
    implicit val chatClient =
      system.actorOf(ChatClient.props(s"client-$name", chatServer), s"client-$name")
    chatClients += name -> chatClient
    // Join message is sent in ChatClient constructor
//    chatServer ! Join(name)
    HttpResponse(OK, entity = s"joined: $name\n")
  }
  def onLeave(name: String) = {
    chatClients.get(name) map { implicit chatClient =>
      chatServer ! Leave
      chatClients -= name
      HttpResponse(OK, entity = s"left: $name\n")
    } getOrElse HttpResponse(NotFound, entity = s"not found: $name\n")
  }
  def onBroadcast(name: String, msg: String) = {
    chatClients.get(name) map { implicit chatClient =>
      chatServer ! Broadcast(msg)
      HttpResponse(OK, entity = s"broadcasted: $msg\n")
    } getOrElse HttpResponse(NotFound, entity = s"not found: $name\n")
  }
  def onShutdown = {
    system.scheduler.scheduleOnce(500 millis)(system.shutdown)
    HttpResponse(OK, entity = s"shutdown: ${system.name} (chatters: ${chatClients.size})\n")
  }

  val chatRoutes = ChatRoutes(onJoin, onLeave, onBroadcast, onShutdown)

  implicit val materializer = ActorFlowMaterializer()

  val requestHandler = Route.handlerFlow(chatRoutes)
//  Http(system).bindAndstartHandlingWith(requestHandler, interface = "localhost", port = 8080)


  val serverSource = Http(system).bind(interface = "localhost", port = 8080)


//  import akka.http.model.HttpMethods._
//  import akka.stream.scaladsl.{ Flow, Sink }
//  val requestHandler: HttpRequest => HttpResponse = {
//    case HttpRequest(GET, Uri.Path("/"), _, _, _) =>
//      HttpResponse(
//        entity = HttpEntity(MediaTypes.`text/html`,
//          "<html><body>Hello world!</body></html>"))
//    case HttpRequest(GET, Uri.Path("/ping"), _, _, _) => HttpResponse(entity = "PONG!")
//    case HttpRequest(GET, Uri.Path("/crash"), _, _, _) => sys.error("BOOM!")
//    case HttpRequest(GET, Uri.Path("/stop"), _, _, _) =>
//      system.shutdown()
//      HttpResponse(entity = "shutting down http server ...")
//    case _: HttpRequest => HttpResponse(404, entity = "Unknown resource!")
//  }
  val bindingFuture = serverSource.to(Sink.foreach { connection =>
    println("Accepted new connection from " + connection.remoteAddress)
    connection handleWith requestHandler
//    connection handleWithSyncHandler requestHandler
    // this is equivalent to
    // connection handleWith { Flow[HttpRequest] map requestHandler }
  }).run()

//  system.shutdown
}
