package org.sandbox.chat.http

import scala.concurrent.duration.DurationInt

import org.sandbox.chat.ChatClient
import org.sandbox.chat.ChatServer.Broadcast
import org.sandbox.chat.ChatServer.Leave

import akka.actor.ActorRef
import akka.actor.ActorSystem
import akka.actor.actorRef2Scala
import akka.http.model.HttpEntity.apply
import akka.http.model.HttpResponse
import akka.http.model.StatusCodes.NotFound
import akka.http.model.StatusCodes.OK

class HttpChatServerActions(chatServer: ActorRef, system: ActorSystem) {

  import system.dispatcher

  var chatClients: Map[String,ActorRef] = Map.empty

  private def chatterNames = chatClients.keySet.toSeq.sorted

  private def ok(msg: String) = HttpResponse(OK, entity = msg)
  private def notFound(name: String) = HttpResponse(NotFound, entity = s"not found: $name\n")

  def onJoin(name: String) = {
    implicit val chatClient =
      system.actorOf(ChatClient.props(s"client-$name", chatServer), s"client-$name")
    chatClients += name -> chatClient
    // Join message is sent in ChatClient constructor
//    chatServer ! Join(name)
    ok(s"joined: $name\n")
  }
  def onLeave(name: String) = {
    chatClients.get(name) map { implicit chatClient =>
      chatServer ! Leave
      chatClients -= name
      ok(s"left: $name\n")
    } getOrElse notFound(name)
  }
  def onBroadcast(name: String, msg: String) = {
    chatClients.get(name) map { implicit chatClient =>
      chatServer ! Broadcast(msg)
      ok(s"broadcasted: $msg\n")
    } getOrElse notFound(name)
  }
  def onShutdown = {
    system.scheduler.scheduleOnce(500 millis)(system.shutdown)
    HttpResponse(OK, entity =
      s"shutdown: ${system.name} (chatters: ${chatterNames.mkString(",")})\n")
  }

}
