package org.sandbox.chat.http

import org.sandbox.chat.ChatServer.Ack
import org.sandbox.chat.ChatServer.Ackable

import akka.actor.Actor
import akka.actor.Props

class AckReceiver(onAck: Ack => Unit) extends Actor {

  override def receive: Receive = {
    case ack: Ack =>
      onAck(ack)
      context.stop(self)
  }
}

object AckReceiver {
  def props(onAck: Ack => Unit): Props =
    Props(new AckReceiver(onAck))
}
