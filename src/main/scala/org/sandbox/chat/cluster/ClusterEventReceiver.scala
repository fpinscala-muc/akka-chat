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

  var clusterMembers: Set[Member] = Set.empty

  implicit val timeout: Timeout

  // subscribe to cluster changes, MemberUp
  // re-subscribe when restart
  override def preStart(): Unit = cluster.subscribe(self, classOf[MemberUp])
  override def postStop(): Unit = cluster.unsubscribe(self)

  def clusterEventReceive: Receive = LoggingReceive {
    case state: CurrentClusterState =>
      val upMembers =
        state.members.filter(m => m.status == MemberStatus.Up && !clusterMembers.contains(m))
      clusterMembers ++= upMembers
      upMembers foreach onMemberUp
    case MemberUp(m) if !clusterMembers.contains(m) =>
      clusterMembers += m
      onMemberUp(m)
  }

  def onMemberUp(member: Member): Unit

  def terminationReceive: Receive = LoggingReceive {
    case Terminated(actor) => onTerminated(actor)
  }

  def onTerminated(actor: ActorRef): Unit

  def isOwnMember(member: Member): Boolean =
    self.path.root == RootActorPath(member.address)

  import context.dispatcher

  def getActor(member: Member, actorName: String): ActorRef = {
    //Option[ActorRef] = { TODO bring back this return value
    def resolveActor: Option[ActorRef] = {
      val actorPath = RootActorPath(member.address) / "user" / actorName
      log.info(s"selecting actor $actorPath")
      val actorSelection = context.actorSelection(actorPath)
      val actorF = actorSelection.resolveOne
      actorF onFailure { case e =>
        log.error(s"could not resolve actor $actorPath: ${e.getMessage}")
      }
      val actor = Try(Await.result(actorF, timeout.duration))
      actor.get // TODO remove this line
      actor.toOption
    }

    val actor = resolveActor
    actor foreach { a =>
      context watch a
      log.info(s"watching ${a.path}")
    }
    actor.get // TODO return Option[ActorRef]
  }
}
