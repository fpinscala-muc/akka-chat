package org.sandbox.chat.cluster

import scala.util.Random

import org.sandbox.chat.ChatService

import akka.actor.ActorRef
import akka.cluster.Cluster
import akka.cluster.Member

class ChatServiceCluster extends ChatService with ClusterEventReceiver {
  var chatMsgPublishers: Set[ActorRef] = Set.empty
  override def chatMsgPublisher: ActorRef = oneOf(chatMsgPublishers)

  var broadcastManagers: Set[ActorRef] = Set.empty
  override def broadcastManager: ActorRef = oneOf(broadcastManagers)

  var participantAdmins: Set[ActorRef] = Set.empty
  override def participantAdmin: ActorRef = oneOf(participantAdmins)

  private def oneOf(actors: Set[ActorRef]): ActorRef =
    Random.shuffle(actors.toSeq).headOption getOrElse(throw new Exception("no actors available"))

  override val shutdownSystem: Boolean = true

  override val cluster = Cluster(context.system)

  override def onTerminated(actor: ActorRef): Unit = {
    chatMsgPublishers = chatMsgPublishers.filterNot(_ == actor)
    broadcastManagers = broadcastManagers.filterNot(_ == actor)
    participantAdmins = participantAdmins.filterNot(_ == actor)
  }

  def receive = clusterEventReceive orElse terminationReceive orElse
                terminationReceive orElse chatServiceReceive

  override def onMemberUp(member: Member): Unit = {
    if (member.hasRole(ChatMsgPublisherRole))
      getActor(member, ChatMsgPublisherRole)  foreach (chatMsgPublishers += _)
    if (member.hasRole(BroadcastManagerRole))
      getActor(member, BroadcastManagerRole) foreach (broadcastManagers += _)
    if (member.hasRole(ParticipantAdministratorRole))
      getActor(member, ParticipantAdministratorRole) foreach (participantAdmins += _)
  }
}

object ChatServiceCluster extends ChatCluster[ChatServiceCluster](ChatServiceRole)
