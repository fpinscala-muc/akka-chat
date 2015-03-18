package org.sandbox.chat.http

import org.sandbox.chat.ChatServer.Ack
import org.sandbox.chat.ChatServer.Ackable
import akka.actor.Actor
import akka.actor.Props
import scala.concurrent.duration.FiniteDuration

class AckReceiver(ack: Ack, onAck: => Unit, timeout: FiniteDuration, onTimeout: => Unit) extends Actor {

  import AckReceiver._
  import context.dispatcher

  val timeoutCancellable =
    context.system.scheduler.scheduleOnce(timeout, self, AckTimeout(ack))

  override def receive: Receive = {
    case ackMsg if ackMsg == ack =>
      onAck
      context.stop(self)
    case ackTimeout: AckTimeout =>
      onTimeout
      context.stop(self)
  }
}

object AckReceiver {
  def props(ack: Ack, onAck: => Unit, timeout: FiniteDuration, onTimeout: => Unit): Props =
    Props(new AckReceiver(ack, onAck, timeout, onTimeout))

  sealed trait AckReceiverMsg
  case class AckTimeout(ack: Ack) extends AckReceiverMsg
}
