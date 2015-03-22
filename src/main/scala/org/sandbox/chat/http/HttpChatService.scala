package org.sandbox.chat.http

import org.sandbox.chat.ServiceActor
import org.sandbox.chat.SettingsActor

import akka.actor.Actor
import akka.actor.ActorLogging
import akka.actor.ActorRef
import akka.actor.Props
import akka.http.Http
import akka.http.marshalling.ToResponseMarshallable
import akka.http.server.Route
import akka.stream.scaladsl.ImplicitFlowMaterializer
import akka.stream.scaladsl.Sink

object HttpChatService {
  def props[T <% ToResponseMarshallable](
        interface: String, port: Int,
        chatServer: ActorRef,
        chatServiceActions: HttpChatServiceActions[T]): Props =
    Props(new HttpChatService(interface, port, chatServer, chatServiceActions))
}

class HttpChatService[T <% ToResponseMarshallable](
    interface: String, port: Int,
    chatServer: ActorRef,
    chatServiceActions: HttpChatServiceActions[T])
  extends Actor with ServiceActor with SettingsActor with ImplicitFlowMaterializer with ActorLogging
{
  import HttpChatService._
  import context.dispatcher
  val chatRoutes = ChatRoutes(chatServiceActions)

  val requestHandler = Route.handlerFlow(chatRoutes)
  val serverSource = Http(context.system).bind(interface = interface, port = port)

//  Http()(context.system)
//    .bindAndstartHandlingWith(requestHandler, interface = interface, port = port)

  val bindingFuture = serverSource.to(Sink.foreach { connection =>
    log.info(s"HttpChatService: accepted new connection from ${connection.remoteAddress}")
    connection handleWith requestHandler
  }).run()

  println(s"HttpChatService listening on $interface:$port")
  println(s"To shutdown, send GET request to http://$interface:$port/shutdown/shutdown")

  override def postStop = {
    println(s"HttpChatService [$interface:$port] shutting down ...")
    super.postStop
  }

  override def receive = serviceReceive

}
