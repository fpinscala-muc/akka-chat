package org.sandbox.chat

import java.util.Date

import akka.actor.Actor
import akka.actor.ActorRef
import akka.actor.Props
import akka.actor.actorRef2Scala
import akka.event.LoggingReceive

class ChatServer(publisher: ActorRef) extends Actor with ServiceActor {
  import ChatServer._

  var participants: Set[Participant] = Set.empty

  private def chatReceive: Receive = {
    case join@Join(who) =>
      participants += who
      sender ! Ack(join)
    case leave@Leave(who) =>
      participants -= who
      sender ! Ack(leave)
    case contribution@Contribution(author@Participant(_,name), msg) if participants contains author =>
      self ! Broadcast(name, msg)
      sender ! Ack(contribution)
    case broadcast: Broadcast =>
      participants foreach(_.who ! broadcast)
      publisher ! broadcast
  }

  override def receive: Receive = LoggingReceive {
    chatReceive orElse serviceReceive
  }
}

object ChatServer {
  def props(publisher: ActorRef): Props =
    Props(new ChatServer(publisher))

  case class Participant(who: ActorRef, name: String)

  trait Ackable

  sealed trait ChatServerMsg
  case class Join(who: Participant) extends ChatServerMsg with Ackable
  case class Leave(who: Participant) extends ChatServerMsg with Ackable
  case class Contribution(author: Participant, msg: String) extends ChatServerMsg with Ackable
  case class Broadcast(authorName: String, msg: String, when: Date  = new Date) extends ChatServerMsg
  case class Ack(what: Ackable) extends ChatServerMsg
}