package org.sandbox.chat.cluster

import org.sandbox.chat.BroadcastManaging

import akka.actor.ActorRef
import akka.cluster.Cluster
import akka.cluster.Member

class BroadcastManagerCluster extends BroadcastManaging with ClusterEventReceiver {

  val participantAdmins =
    new ChatClusterActors(ParticipantAdministratorRole, context, timeout, log)
  override def participantAdmin: ActorRef =
    participantAdmins.randomActor getOrElse(throw new Exception("no participantAdmins available"))

  override val cluster = Cluster(context.system)

  def receive: Receive = broadcastReceive orElse clusterEventReceive orElse terminationReceive

  override def onMemberUp(member: Member): Unit = participantAdmins.onMemberDown(member)
  override def onMemberDown(member: Member): Unit = participantAdmins.onMemberDown(member)
  override def onTerminated(actor: ActorRef): Unit = participantAdmins.onTerminated(actor)
}

object BroadcastManagerCluster extends ChatCluster[BroadcastManagerCluster](BroadcastManagerRole)
