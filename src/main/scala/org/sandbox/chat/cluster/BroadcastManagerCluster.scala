package org.sandbox.chat.cluster

import scala.concurrent.duration.DurationInt

import org.sandbox.chat.BroadcastManaging
import akka.actor.ActorRef
import scala.util.Random
import akka.cluster.Member
import akka.util.Timeout
import akka.cluster.Cluster

class BroadcastManagerCluster extends BroadcastManaging with ClusterEventReceiver {

  var participantAdmins: Set[ActorRef] = Set.empty
  override def participantAdmin: ActorRef = oneOf(participantAdmins)

  private def oneOf(actors: Set[ActorRef]): ActorRef =
    Random.shuffle(actors.toSeq).headOption getOrElse(throw new Exception("no actors available"))

  override val cluster = Cluster(context.system)

  override implicit val timeout = Timeout(1 second)

  def receive: Receive = broadcastReceive orElse clusterEventReceive orElse terminationReceive

  def onMemberUp(member: Member): Unit = {
    if (member.hasRole(ParticipantAdministratorRole))
      getActor(member, ParticipantAdministratorRole) foreach (participantAdmins += _)
  }

  override def onTerminated(actor: ActorRef): Unit = {
    participantAdmins = participantAdmins.filterNot(_ == actor)
  }
}

object BroadcastManagerCluster extends ChatCluster[BroadcastManagerCluster](BroadcastManagerRole)
