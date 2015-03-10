package org.sandbox.chat.http

import akka.actor.ActorSystem
import akka.http.Http
import akka.http.server.Route
import akka.stream.ActorFlowMaterializer
import akka.stream.scaladsl.Sink

object HttpChatApp extends App {

  implicit val system = ActorSystem("chat-http")
  implicit val materializer = ActorFlowMaterializer()

  import system.dispatcher
  val requestHandler = Route.handlerFlow(ChatRoutes.route)
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
}
