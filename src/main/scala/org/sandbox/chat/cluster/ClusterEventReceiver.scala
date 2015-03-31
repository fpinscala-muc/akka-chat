package org.sandbox.chat.cluster

import scala.concurrent.Await
import scala.util.Try

import akka.actor.Actor
import akka.actor.ActorLogging
import akka.actor.ActorRef
import akka.actor.RootActorPath
import akka.actor.Terminated
import akka.cluster.Cluster
import akka.cluster.ClusterEvent.CurrentClusterState
import akka.cluster.ClusterEvent.MemberExited
import akka.cluster.ClusterEvent.MemberRemoved
import akka.cluster.ClusterEvent.MemberUp
import akka.cluster.ClusterEvent.ReachableMember
import akka.cluster.ClusterEvent.UnreachableMember
import akka.cluster.Member
import akka.cluster.MemberStatus
import akka.event.LoggingReceive
import akka.util.Timeout

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
  override def preStart(): Unit =
    cluster.subscribe(self, classOf[MemberUp],
        classOf[UnreachableMember], classOf[ReachableMember],
        classOf[MemberExited], classOf[MemberRemoved])
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
    case UnreachableMember(m) =>
      clusterMembers -= m
      onMemberDown(m)
    case ReachableMember(m) =>
      clusterMembers += m
      onMemberUp(m)
    case MemberExited(m) =>
      clusterMembers -= m
      onMemberDown(m)
    case MemberRemoved(m, _) =>
      clusterMembers -= m
      onMemberDown(m)
  }

  def onMemberUp(member: Member): Unit
  def onMemberDown(member: Member): Unit

  def terminationReceive: Receive = LoggingReceive {
    case Terminated(actor) => onTerminated(actor)
  }

  def onTerminated(actor: ActorRef): Unit

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
