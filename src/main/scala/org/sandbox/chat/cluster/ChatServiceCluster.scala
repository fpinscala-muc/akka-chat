package org.sandbox.chat.cluster

import scala.util.Random

import org.sandbox.chat.ChatService

import akka.actor.ActorRef
import akka.cluster.Cluster
import akka.cluster.Member

class ChatServiceCluster extends ChatService with ClusterEventReceiver with ActorResolver {
  val chatMsgPublishers =
    new ChatClusterActors(ChatMsgPublisherRole, context, timeout, log)
  override def chatMsgPublisher: ActorRef =
    chatMsgPublishers.randomActor getOrElse(throw new Exception("no chatMsgPublishers available"))

  val broadcastManagers =
    new ChatClusterActors(BroadcastManagerRole, context, timeout, log)
  override def broadcastManager: ActorRef =
    broadcastManagers.randomActor getOrElse(throw new Exception("no broadcastManagers available"))

  val participantAdmins =
    new ChatClusterActors(ParticipantAdministratorRole, context, timeout, log)
  override def participantAdmin: ActorRef =
    participantAdmins.randomActor getOrElse(throw new Exception("no participantAdmins available"))

  private def oneOf(actors: Set[ActorRef]): ActorRef =
    Random.shuffle(actors.toSeq).headOption getOrElse(throw new Exception("no actors available"))

  override def doShutdown = {
    val reaperPath = self.path.root / "user" / ClusterReaperRole.name
    val reaper =
      resolveActor(reaperPath)(context, timeout, log) getOrElse(throw new Exception(s"reaper $reaperPath not found"))
    reaper ! ChatClusterReaper.Shutdown
  }

  override val cluster = Cluster(context.system)

  override def onTerminated(actor: ActorRef): Unit = {
    chatMsgPublishers.onTerminated(actor)
    broadcastManagers.onTerminated(actor)
    participantAdmins.onTerminated(actor)
  }

  def receive =
    clusterEventReceive orElse terminationReceive orElse
    terminationReceive orElse chatServiceReceive

  override def onMemberUp(member: Member): Unit = {
    chatMsgPublishers.onMemberUp(member)
    broadcastManagers.onMemberUp(member)
    participantAdmins.onMemberUp(member)
  }
  override def onMemberDown(member: Member): Unit = {
    chatMsgPublishers.onMemberDown(member)
    broadcastManagers.onMemberDown(member)
    participantAdmins.onMemberDown(member)
  }
}

object ChatServiceCluster extends ChatCluster[ChatServiceCluster](ChatServiceRole)
