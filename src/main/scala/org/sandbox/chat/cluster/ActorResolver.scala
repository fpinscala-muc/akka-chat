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

  def resolveActor(actorPath: ActorPath)
      (implicit context: ActorContext, timeout: Timeout, log: LoggingAdapter): Try[ActorRef] = {
    log.info(s"selecting actor $actorPath")
    val actorSelection = context.actorSelection(actorPath)
    val actorF = actorSelection.resolveOne
    import context.dispatcher
    actorF onFailure {
      case e => log.error(s"could not resolve actor $actorPath: ${e.getMessage}")
    }
    Try(Await.result(actorF, timeout.duration))
  }

  def resolveActorForMember(member: Member, actorName: String)
      (implicit context: ActorContext, timeout: Timeout, log: LoggingAdapter): Try[ActorRef] = {
    val actorPath = RootActorPath(member.address) / "user" / actorName
    resolveActor(actorPath)
  }
}
