package org.sandbox.chat

import scala.concurrent.duration.DurationInt
import akka.actor.Actor
import akka.actor.ActorRef
import akka.actor.Props
import akka.pattern.{ ask, pipe }
import akka.util.Timeout

class BroadcastManager(override val participantAdmin: ActorRef) extends BroadcastManaging {
  def receive = broadcastReceive
}

object BroadcastManager {
  def props(participantAdmin: ActorRef): Props =
    Props(new BroadcastManager(participantAdmin))
}
