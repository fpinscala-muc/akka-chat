package org.sandbox.chat.cluster

import scala.annotation.migration
import scala.util.Random

import org.sandbox.chat.cluster.ChatClusterRole.roleToString

import akka.actor.ActorContext
import akka.actor.ActorRef
import akka.cluster.Member
import akka.event.LoggingAdapter
import akka.util.Timeout

class ChatClusterActors(role: ChatClusterRole,
    implicit val context: ActorContext, implicit val timeout: Timeout,
    implicit val log: LoggingAdapter) extends ActorResolver
{
  private var clusterActors: Map[Member,ActorRef] = Map.empty

  def randomActor: Option[ActorRef] =
    Random.shuffle(clusterActors.values.toSeq).headOption

  def foreach(f: ((Member,ActorRef)) => Unit): Unit =
    clusterActors foreach f

  def onMemberUp(member: Member) = {
    if (member.hasRole(role)) {
      val actor = resolveActorForMember(member, role)
      actor foreach { a =>
        clusterActors += member -> a
        context watch a
        log.info(s"watching ${a.path}")
      }
    }
  }

  def onMemberDown(member: Member) =
    clusterActors -= member

  def onTerminated(actor: ActorRef) =
    clusterActors = clusterActors.filterNot { case (_, a) => a == actor }
}
