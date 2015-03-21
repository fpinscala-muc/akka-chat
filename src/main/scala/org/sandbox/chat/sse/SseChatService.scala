package org.sandbox.chat.sse

import scala.concurrent.ExecutionContext

import org.sandbox.chat.SettingsActor
import org.sandbox.chat.http.ServiceActor

import akka.actor.Actor
import akka.actor.ActorLogging
import akka.actor.ActorRef
import akka.actor.Props
import akka.http.Http
import akka.http.marshalling.ToResponseMarshallable.apply
import akka.http.server.Directive.addByNameNullaryApply
import akka.http.server.Directives
import akka.http.server.Route
import akka.stream.ActorFlowMaterializer
import akka.stream.actor.ActorPublisher
import akka.stream.scaladsl.ImplicitFlowMaterializer
import akka.stream.scaladsl.Sink
import akka.stream.scaladsl.Source
import de.heikoseeberger.akkasse.EventStreamMarshalling
import de.heikoseeberger.akkasse.ServerSentEvent

object SseChatService {
  def props(interface: String, port: Int, sseChatPublisher: ActorRef): Props =
    Props(new SseChatService(interface, port, sseChatPublisher))
}

class SseChatService(interface: String, port: Int, sseChatPublisher: ActorRef)
  extends Actor with ServiceActor with SettingsActor with ImplicitFlowMaterializer
  with ActorLogging with Directives with EventStreamMarshalling
{
  import SseChatService._

  val sseSource: Source[ServerSentEvent, _] = getSseSource

  import context.dispatcher
  val requestHandler = Route.handlerFlow(route)
//    Route.asyncHandler(route)
  val serverSource =
    Http(context.system)
      .bind(interface = interface, port = port)
//      .runForeach(_.flow.join(route).run())

  val bindingFuture = serverSource.to(Sink.foreach { connection =>
    log.info(s"SseChatService: accepted new connection from ${connection.remoteAddress}")
    connection handleWith requestHandler
//    connection handleWithAsyncHandler requestHandler
  }).run()

  println(s"SseChatService listening on $interface:$port")

  override def postStop = {
    println(s"SseChatService [$interface:$port] shutting down ...")
    super.postStop
  }

  override def receive = receiveStatus

  private def route(implicit ec: ExecutionContext, mat: ActorFlowMaterializer) =
    get {
      complete(sseSource)
    }

  private def getSseSource = {
    // a normal Publisher can only accept one Subscriber, so we have to fan out
    val sseMultiSubscriberPublisher = Source(ActorPublisher[ServerSentEvent](sseChatPublisher))
      .runWith(Sink.fanoutPublisher(initialBufferSize = 8, maximumBufferSize = 16))
    Source(sseMultiSubscriberPublisher)
  }
}