package org.sandbox.chat.http

import org.sandbox.chat.ServiceActor
import org.sandbox.chat.SettingsActor

import akka.actor.Actor
import akka.actor.ActorLogging
import akka.http.Http
import akka.http.model.HttpResponse
import akka.http.server.Route
import akka.stream.scaladsl.ImplicitFlowMaterializer
import akka.stream.scaladsl.Sink

trait HttpChatServing
  extends Actor with ServiceActor with SettingsActor with ImplicitFlowMaterializer with ActorLogging
{
  val interface = settings.httpService.interface
  val port = settings.httpService.port

  def chatServiceActions: HttpChatServiceActions[HttpResponse]

  import HttpChatService._
  import context.dispatcher

  var serviceStarted = false

  private def startService = {
    val chatRoutes = ChatRoutes(chatServiceActions)

    val requestHandler = Route.handlerFlow(chatRoutes)
    val serverSource = Http(context.system).bind(interface = interface, port = port)

    //  Http()(context.system)
    //    .bindAndstartHandlingWith(requestHandler, interface = interface, port = port)

    val bindingFuture = serverSource.to(Sink.foreach { connection =>
      log.info(s"HttpChatService: accepted new connection from ${connection.remoteAddress}")
      connection handleWith requestHandler
    }).run()

    serviceStarted = true

    println(s"HttpChatService listening on $interface:$port")
    println(s"To shutdown, send GET request to http://$interface:$port/shutdown/shutdown")
  }

  import HttpChatServing._

  def startServiceReceive: Receive = {
    case StartHttpService if !serviceStarted => startService
  }

  self ! StartHttpService

  override def postStop = {
    println(s"HttpChatService [$interface:$port] shutting down ...")
    super.postStop
  }
}

object HttpChatServing {
  object StartHttpService
}