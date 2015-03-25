package org.sandbox.chat.http

import org.sandbox.chat.ChatServer
import org.sandbox.chat.ServiceActor
import org.sandbox.chat.Settings

import akka.actor.ActorRef
import akka.actor.ActorSystem
import akka.actor.Props

trait HttpChat {

  implicit val system = ActorSystem("chat-http")
  val settings = Settings(system)

  def onReady: Unit = {}

//  def shutdown = {
//    system.shutdown
//    system.awaitTermination
//  }

  val chatServer = system.actorOf(ChatServer.props, "ChuckNorris")
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
