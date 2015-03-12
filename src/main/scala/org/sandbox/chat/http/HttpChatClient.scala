package org.sandbox.chat.http

import org.sandbox.chat.ChatServer.Broadcast

import akka.actor.Actor
import akka.actor.ActorRef
import akka.actor.Props
import akka.actor.actorRef2Scala
import akka.event.LoggingReceive
import akka.pattern.ask
import akka.util.Timeout
import scala.concurrent.duration.DurationInt
import akka.pattern.pipe

class HttpChatClient private(chatServer: ActorRef) extends Actor {
  var broadcasts: Seq[String] = Seq.empty

  import HttpChatClient._

  import context.dispatcher
  implicit val timeout = Timeout(1 second)

  def receive: Actor.Receive = LoggingReceive {
    case Broadcast(msg) if sender == chatServer =>
      broadcasts = broadcasts :+ msg
    case GetBroadcasts =>
      sender ! Broadcasts(broadcasts)
      broadcasts = Seq.empty
    case msg =>
      val future = chatServer ? msg
      future pipeTo sender
  }
}

object HttpChatClient {
  def props(chatServer: ActorRef): Props =
    Props(new HttpChatClient(chatServer))

  sealed trait HttpChatClientMsg
  case object GetBroadcasts extends HttpChatClientMsg
  case class Broadcasts(messages: Seq[String])
}
