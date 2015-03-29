package org.sandbox.chat.cluster

import scala.concurrent.duration.DurationInt

import org.sandbox.chat.ChatMsgPublishing

import akka.actor.ActorRef
import akka.cluster.Cluster
import akka.cluster.Member
import akka.util.Timeout

class ChatMsgPublisherCluster extends ChatMsgPublishing with ClusterEventReceiver {

  var chatMsgPublishers: Set[ActorRef] = Set.empty

  override val cluster = Cluster(context.system)

  override implicit val timeout = Timeout(1 second)

  def receive: Receive = chatMsgReceive orElse clusterEventReceive orElse terminationReceive

  def onMemberUp(member: Member): Unit = {
    if (member.hasRole("chatMsgPublisher"))
      chatMsgPublishers += getActor(member, "chatMsgPublisher")
  }

  override def onTerminated(actor: ActorRef): Unit = {
    chatMsgPublishers = chatMsgPublishers.filterNot(_ == actor)
  }
}

object ChatMsgPublisherCluster extends ChatCluster[ChatMsgPublisherCluster]("chatMsgPublisher")
