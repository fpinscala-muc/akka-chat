package org.sandbox.chat.cluster

import scala.concurrent.Await
import akka.actor.Actor
import akka.actor.ActorLogging
import akka.actor.ActorRef
import akka.actor.RootActorPath
import akka.actor.Terminated
import akka.cluster.Cluster
import akka.cluster.ClusterEvent.CurrentClusterState
import akka.cluster.ClusterEvent.MemberUp
import akka.cluster.Member
import akka.cluster.MemberStatus
import akka.event.LoggingReceive
import akka.util.Timeout
import scala.util.Try

trait ClusterEventReceiver extends Actor with ActorLogging {

  val cluster: Cluster

  def isSelfMember(member: Member) =
    member.address == cluster.selfAddress

  def isNewMember(member: Member) =
    !isSelfMember(member) && !clusterMembers.contains(member)

  var clusterMembers: Set[Member] = Set.empty

  implicit val timeout: Timeout

  // subscribe to cluster changes, MemberUp
  // re-subscribe when restart
  override def preStart(): Unit = cluster.subscribe(self, classOf[MemberUp])
  override def postStop(): Unit = cluster.unsubscribe(self)

  def clusterEventReceive: Receive = LoggingReceive {
    case state: CurrentClusterState =>
      val upMembers =
        state.members.filter(m => m.status == MemberStatus.Up && isNewMember(m))
      clusterMembers ++= upMembers
      upMembers foreach onMemberUp
    case MemberUp(m) if isNewMember(m) =>
      clusterMembers += m
      onMemberUp(m)
  }

  def onMemberUp(member: Member): Unit

  def terminationReceive: Receive = LoggingReceive {
    case Terminated(actor) => onTerminated(actor)
  }

  def onTerminated(actor: ActorRef): Unit

//  def isOwnMember(member: Member): Boolean = {
//    println(s"$member $clusterMembers")
//    println(s"${self.path.root.toSerializationFormat} ${RootActorPath(member.address).toSerializationFormat} ${self.path.root.address == member.address}")
//    val i = self.path.root compareTo RootActorPath(member.address)
//    println(s"i=$i")
//    i == 0
//  }

  import context.dispatcher

  def getActor(member: Member, actorName: String): Option[ActorRef] = {
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
