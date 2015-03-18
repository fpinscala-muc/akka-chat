package org.sandbox.chat.sse

import akka.actor.ActorSystem
import akka.http.Http
import akka.http.server.Directives
import akka.stream.ActorFlowMaterializer
import scala.concurrent.ExecutionContext
import de.heikoseeberger.akkasse.ServerSentEvent
import akka.stream.scaladsl.Source
import de.heikoseeberger.akkasse.EventStreamMarshalling

class SseChatService(sseSource: Source[ServerSentEvent, _],
    implicit val system: ActorSystem, implicit val mat: ActorFlowMaterializer)
   extends Directives with EventStreamMarshalling
{
  import system.dispatcher

  Http()
    .bind("127.0.0.1", 9000)
    .runForeach(_.flow.join(route).run())

  private def route(implicit ec: ExecutionContext, mat: ActorFlowMaterializer) =
    get {
      complete(sseSource)
    }
}