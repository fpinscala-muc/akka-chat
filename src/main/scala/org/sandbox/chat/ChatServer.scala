package org.sandbox.chat

import akka.actor.Actor
import akka.actor.ActorRef
import ChatServer._

class ChatServer extends Actor {
  var chatters: Map[ActorRef,String] = Map.empty

  override def receive: Actor.Receive = {
    case Join(name) => chatters += sender -> name
    case Leave => chatters -= sender
    case Broadcast(msg) =>
      for {
        senderName <- chatters.get(sender)
      }
      chatters.keys foreach (_ ! Broadcast(s"$senderName: $msg"))
  }
}

object ChatServer {
  sealed trait ChatServerMsg
  case class Join(name: String) extends ChatServerMsg
  case object Leave
  case class Broadcast(msg: String)
}