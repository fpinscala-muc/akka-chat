package org.sandbox.chat.http

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt

import akka.actor.Actor
import akka.actor.ActorRef
import akka.pattern.ask
import akka.util.Timeout

trait ServiceActor {
  this: Actor =>

  import ServiceActor._

  def receiveStatus: Receive = {
    case GetStatus => sender ! StatusRunning
  }
}

object ServiceActor {

  sealed trait ServiceMsg
  object GetStatus extends ServiceMsg
  trait Status extends ServiceMsg
  object StatusRunning extends Status

  implicit val timeout = Timeout(1 second)

  def getStatus(service: ActorRef): Status = {
    val statusF = ask(service, GetStatus).mapTo[Status]
    Await.result(statusF, timeout.duration)
  }
}
