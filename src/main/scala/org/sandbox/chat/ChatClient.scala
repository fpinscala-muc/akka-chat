package org.sandbox.chat

import akka.actor.Actor
import ChatServer._
import akka.actor.ActorRef

class ChatClient(name: String, chatServer: ActorRef) extends Actor {
  def receive: Actor.Receive = {
    case Broadcast(msg) =>
      println(s"$name: received $msg")
  }

  chatServer ! Join(name)
  chatServer ! Broadcast(s"Hi! I'm $name")
}