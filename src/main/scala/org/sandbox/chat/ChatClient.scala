package org.sandbox.chat

import ChatServer.Broadcast
import ChatServer.Contribution
import ChatServer.Join
import ChatServer.Participant
import akka.actor.Actor
import akka.actor.ActorRef
import akka.actor.Props
import akka.actor.actorRef2Scala

class ChatClient private(name: String, chatServer: ActorRef) extends Actor {
  import ChatServer._

  def receive: Actor.Receive = {
    case Broadcast(author, msg, when) =>
      println(s"$author: received $msg")
  }

  chatServer ! Join(Participant(self, name))
  chatServer ! Contribution(Participant(self, name), s"Hi! I'm $name")
}

object ChatClient {
  def props(name: String, chatServer: ActorRef): Props =
    Props(new ChatClient(name, chatServer))
}