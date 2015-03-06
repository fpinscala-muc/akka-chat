package org.sandbox.chat

import ChatServer.{Broadcast,Join}
import akka.actor.Actor
import akka.actor.ActorRef
import akka.actor.Props
import akka.actor.actorRef2Scala

class ChatClient private(name: String, chatServer: ActorRef) extends Actor {
  import ChatServer._

  def receive: Actor.Receive = {
    case Broadcast(msg) =>
      println(s"$name: received $msg")
  }

  chatServer ! Join(name)
  chatServer ! Broadcast(s"Hi! I'm $name")
}

object ChatClient {
  def props(name: String, chatServer: ActorRef): Props =
    Props(new ChatClient(name, chatServer))
}