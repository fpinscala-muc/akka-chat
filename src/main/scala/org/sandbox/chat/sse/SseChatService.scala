package org.sandbox.chat.sse

import scala.concurrent.ExecutionContext

import akka.actor.ActorSystem
import akka.http.Http
import akka.http.server.Directives
import akka.http.server.Route
import akka.stream.ActorFlowMaterializer
import akka.stream.scaladsl.Sink
import akka.stream.scaladsl.Source
import de.heikoseeberger.akkasse.EventStreamMarshalling
import de.heikoseeberger.akkasse.ServerSentEvent

class SseChatService(sseSource: Source[ServerSentEvent, _],
    implicit val system: ActorSystem, implicit val mat: ActorFlowMaterializer)
   extends Directives with EventStreamMarshalling
{
  import system.dispatcher

  val host = "127.0.0.1"
  val port = 9000

  val requestHandler = Route.handlerFlow(route)
//    Route.asyncHandler(route)
  val serverSource =
    Http(system)
      .bind(interface = host, port = port)
//      .runForeach(_.flow.join(route).run())

  val bindingFuture = serverSource.to(Sink.foreach { connection =>
    system.log.info(s"SseChatService: accepted new connection from ${connection.remoteAddress}")
    connection handleWith requestHandler
//    connection handleWithAsyncHandler requestHandler
  }).run()

  println(s"SseChatService listening on $host:$port")

  private def route(implicit ec: ExecutionContext, mat: ActorFlowMaterializer) =
    get {
      complete(sseSource)
    }
}