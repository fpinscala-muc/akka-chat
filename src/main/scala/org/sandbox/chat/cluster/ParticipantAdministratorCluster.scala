package org.sandbox.chat.cluster

import scala.concurrent.duration.DurationInt

import org.sandbox.chat.ParticipantAdministrating
import akka.cluster.Member
import akka.util.Timeout
import akka.actor.ActorRef
import akka.cluster.Cluster

class ParticipantAdministratorCluster extends ParticipantAdministrating with ClusterEventReceiver {

  override val cluster = Cluster(context.system)

  override implicit val timeout = Timeout(1 second)

  val participantAdmins =
    new ChatClusterActors(ParticipantAdministratorRole, context, timeout, log)

  def receive: Receive = participantReceive orElse clusterEventReceive orElse terminationReceive

  override def onMemberUp(member: Member): Unit = participantAdmins.onMemberUp(member)
  override def onMemberDown(member: Member): Unit = participantAdmins.onMemberDown(member)

  override def onTerminated(actor: ActorRef): Unit = participantAdmins.onTerminated(actor)
}

object ParticipantAdministratorCluster extends ChatCluster[ParticipantAdministratorCluster](ParticipantAdministratorRole)
