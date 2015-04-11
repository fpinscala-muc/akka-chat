package org.sandbox.chat.http

import org.sandbox.chat.ChatServer
import org.sandbox.chat.ServiceActor

import akka.actor.ActorRef
import akka.actor.ActorSystem

trait HttpChat {

  implicit val system = ActorSystem("chat-http")

  def onReady: Unit = {}

//  def shutdown = {
//    system.shutdown
//    system.awaitTermination
//  }

  val chatServer = system.actorOf(ChatServer.props(true), "ChuckNorris")
  waitForRunningService(chatServer)

  system.log.info(s"HttpChatApp with ActorSystem ${system.name} started")
  system.registerOnTermination(system.log.info(s"ActorSystem ${system.name} shutting down ..."))

  onReady

  system.awaitTermination

  private def waitForRunningService(service: ActorRef) = {
    val status = ServiceActor.getStatus(service)
    require(status == ServiceActor.StatusRunning)
  }
}

object HttpChatApp extends App with HttpChat
