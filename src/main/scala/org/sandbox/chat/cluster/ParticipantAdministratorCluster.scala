package org.sandbox.chat.cluster

import scala.concurrent.duration.DurationInt

import org.sandbox.chat.ParticipantAdministrating
import akka.cluster.Member
import akka.util.Timeout
import akka.actor.ActorRef
import akka.cluster.Cluster

class ParticipantAdministratorCluster extends ParticipantAdministrating with ClusterEventReceiver {

  var participantAdmins: Set[ActorRef] = Set.empty

  override val cluster = Cluster(context.system)

  override implicit val timeout = Timeout(1 second)

  def receive: Receive = participantReceive orElse clusterEventReceive orElse terminationReceive

  def onMemberUp(member: Member): Unit = {
    if (member.hasRole(ParticipantAdministratorRole))
      getActor(member, ParticipantAdministratorRole) foreach (participantAdmins += _)
  }

  override def onTerminated(actor: ActorRef): Unit = {
    participantAdmins = participantAdmins.filterNot(_ == actor)
  }
}

object ParticipantAdministratorCluster extends ChatCluster[ParticipantAdministratorCluster](ParticipantAdministratorRole)
