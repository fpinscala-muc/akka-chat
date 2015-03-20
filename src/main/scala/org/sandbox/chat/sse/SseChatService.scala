package org.sandbox.chat.sse

import scala.concurrent.ExecutionContext

import akka.actor.ActorRef
import akka.actor.ActorSystem
import akka.actor.Props
import akka.http.Http
import akka.http.marshalling.ToResponseMarshallable.apply
import akka.http.server.Directive.addByNameNullaryApply
import akka.http.server.Directives
import akka.http.server.Route
import akka.stream.ActorFlowMaterializer
import akka.stream.actor.ActorPublisher
import akka.stream.scaladsl.Sink
import akka.stream.scaladsl.Source
import de.heikoseeberger.akkasse.EventStreamMarshalling
import de.heikoseeberger.akkasse.ServerSentEvent

class SseChatService(implicit val system: ActorSystem, implicit val mat: ActorFlowMaterializer)
   extends Directives with EventStreamMarshalling
{
  val sseChatPublisher: ActorRef = system.actorOf(Props[SseChatPublisher])
  def getPublisher = sseChatPublisher

  def getChatServerActions(chatServer: ActorRef) =
    new SseChatServerActions(chatServer, sseChatPublisher, system)

  val sseSource: Source[ServerSentEvent, _] = getSseSource(16)

  val host = "127.0.0.1"
  val port = 9000

  import system.dispatcher
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

  private def getSseSource(maxSubscribers: Int) = {
    // a normal Publisher can only accept one Subscriber, so we have to fan out
    val sseMultiSubscriberPublisher = Source(ActorPublisher[ServerSentEvent](sseChatPublisher))
      .runWith(Sink.fanoutPublisher(initialBufferSize = 8, maximumBufferSize = maxSubscribers))
    Source(sseMultiSubscriberPublisher)
  }
}