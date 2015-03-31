package org.sandbox.chat.cluster

import scala.annotation.migration
import scala.concurrent.Await
import scala.util.Random
import scala.util.Try

import org.sandbox.chat.cluster.ChatClusterRole.roleToString

import akka.actor.ActorContext
import akka.actor.ActorRef
import akka.actor.RootActorPath
import akka.cluster.Member
import akka.event.LoggingAdapter
import akka.util.Timeout

class ChatClusterActors(role: ChatClusterRole,
    implicit val context: ActorContext, implicit val timeout: Timeout,
    implicit val log: LoggingAdapter)
{
  var clusterActors: Map[Member,ActorRef] = Map.empty

  def randomActor: Option[ActorRef] =
    Random.shuffle(clusterActors.values.toSeq).headOption

  def onMemberUp(member: Member) = {
    if (member.hasRole(role)) {
      val actor = getActor(member, role)
      actor foreach (clusterActors += member -> _)
    }
  }

  def onMemberDown(member: Member) =
    clusterActors -= member

  def onTerminated(actor: ActorRef) =
    clusterActors = clusterActors.filterNot { case (_, a) => a == actor }

  import context.dispatcher

  private def getActor(member: Member, actorName: String): Option[ActorRef] = {
    def resolveActor: Option[ActorRef] = {
      val actorPath = RootActorPath(member.address) / "user" / actorName
      log.info(s"selecting actor $actorPath")
      val actorSelection = context.actorSelection(actorPath)
      val actorF = actorSelection.resolveOne
      actorF onFailure { case e =>
        log.error(s"could not resolve actor $actorPath: ${e.getMessage}")
      }
      val actor = Try(Await.result(actorF, timeout.duration))
      actor.toOption
    }

    val actor = resolveActor
    actor foreach { a =>
      context watch a
      log.info(s"watching ${a.path}")
    }
    actor
  }
}
