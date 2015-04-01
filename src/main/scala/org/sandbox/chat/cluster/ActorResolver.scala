package org.sandbox.chat.cluster

import scala.concurrent.Await
import scala.util.Try

import akka.actor.ActorContext
import akka.actor.ActorPath
import akka.actor.ActorRef
import akka.actor.RootActorPath
import akka.cluster.Member
import akka.event.LoggingAdapter
import akka.util.Timeout

trait ActorResolver {

  implicit val context: ActorContext
  implicit val timeout: Timeout
  implicit val log: LoggingAdapter

  import context.dispatcher

  def resolveActor(actorPath: ActorPath): Try[ActorRef] = {
    log.info(s"selecting actor $actorPath")
    val actorSelection = context.actorSelection(actorPath)
    val actorF = actorSelection.resolveOne
    actorF onFailure {
      case e => log.error(s"could not resolve actor $actorPath: ${e.getMessage}")
    }
    Try(Await.result(actorF, timeout.duration))
  }

  def resolveActorForMember(member: Member, actorName: String): Try[ActorRef] = {
    val actorPath = RootActorPath(member.address) / "user" / actorName
    resolveActor(actorPath)
  }
}
