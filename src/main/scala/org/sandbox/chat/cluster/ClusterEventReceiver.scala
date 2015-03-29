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

trait ClusterEventReceiver extends Actor with ActorLogging {

  val cluster: Cluster

  implicit val timeout: Timeout

  // subscribe to cluster changes, MemberUp
  // re-subscribe when restart
  override def preStart(): Unit = cluster.subscribe(self, classOf[MemberUp])
  override def postStop(): Unit = cluster.unsubscribe(self)

  def clusterEventReceive: Receive = LoggingReceive {
    case state: CurrentClusterState =>
      state.members.filter(_.status == MemberStatus.Up) foreach onMemberUp
    case MemberUp(m) => onMemberUp(m)
  }

  def onMemberUp(member: Member): Unit

  def terminationReceive: Receive = LoggingReceive {
    case Terminated(actor) => onTerminated(actor)
  }

  def onTerminated(actor: ActorRef): Unit

  def getActor(member: Member, actorName: String): ActorRef = {
    val actorSelection =
      context.actorSelection(RootActorPath(member.address) / "user" / actorName)
    val actor = Await.result(actorSelection.resolveOne, timeout.duration)
    context watch actor
    log.info(s"registered ${actor.path}")
    actor
  }
}
