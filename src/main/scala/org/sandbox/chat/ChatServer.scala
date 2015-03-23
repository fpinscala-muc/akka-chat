package org.sandbox.chat

import java.util.Date

import scala.concurrent.duration.DurationInt

import akka.actor.Actor
import akka.actor.ActorLogging
import akka.actor.ActorRef
import akka.actor.Props
import akka.actor.actorRef2Scala
import akka.event.LoggingReceive

class ChatServer(publisher: ActorRef) extends Actor with ServiceActor with ActorLogging {
  import ChatServer._
  import context.dispatcher

  var participants: Set[Participant] = Set.empty

  private def ackAndPublish(msg: ChatServerMsg with Ackable) = {
    publisher ! msg
    sender ! Ack(msg)
  }

  private def chatReceive: Receive = {
    case join@Join(who) =>
      participants += who
      ackAndPublish(join)
    case leave@Leave(who) =>
      participants -= who
      ackAndPublish(leave)
    case contribution@Contribution(author@Participant(_,name), msg) if participants contains author =>
      self ! Broadcast(name, msg)
      ackAndPublish(contribution)
    case broadcast: Broadcast =>
      participants foreach(_.who ! broadcast)
      publisher ! broadcast
    case shutdown: Shutdown =>
      println("OK")
      ackAndPublish(shutdown.copy(participants = participants))
      log.info(s"ChatServer ${self.path.name} to shutdown in ${500 millis} ...")
      context.system.scheduler.scheduleOnce(500 millis)(context.system.shutdown)
  }

  override def receive: Receive = LoggingReceive {
    chatReceive orElse serviceReceive
  }

  override def postStop = {
    log.info(s"ChatServer ${self.path.name} shutting down ...")
    super.postStop
  }

  log.info(s"ChatServer ${self.path.name} started")
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
  case class Shutdown(participants: Set[Participant] = Set()) extends ChatServerMsg with Ackable
  case class Ack(what: Ackable) extends ChatServerMsg
}