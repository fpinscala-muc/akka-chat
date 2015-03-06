package org.sandbox.chat

import scala.annotation.migration

import akka.actor.Actor
import akka.actor.ActorRef
import akka.actor.actorRef2Scala
import akka.event.LoggingReceive

class ChatServer extends Actor {
  import ChatServer._

  var chatters: Map[ActorRef,String] = Map.empty

  override def receive: Actor.Receive = LoggingReceive {
    case Join(name) => chatters += sender -> name
    case Leave => chatters -= sender
    case Broadcast(msg) =>
      chatters.get(sender) foreach(broadcast(_, msg))
  }

  private def broadcast(senderName: String, msg: String) = {
    val broadcastMsg = Broadcast(s"$senderName: $msg")
    chatters.keys foreach (_ ! broadcastMsg)
  }
}

object ChatServer {
  sealed trait ChatServerMsg
  case class Join(name: String) extends ChatServerMsg
  case object Leave
  case class Broadcast(msg: String)
}