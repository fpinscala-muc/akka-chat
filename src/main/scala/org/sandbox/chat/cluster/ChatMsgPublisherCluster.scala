package org.sandbox.chat.cluster

import scala.concurrent.duration.DurationInt

import org.sandbox.chat.ChatMsgPublishing

import akka.actor.ActorRef
import akka.cluster.Cluster
import akka.cluster.Member
import akka.util.Timeout

class ChatMsgPublisherCluster extends ChatMsgPublishing with ClusterEventReceiver {

  override implicit val timeout = Timeout(1 second)

  val chatMsgPublishers =
    new ChatClusterActors(ChatMsgPublisherRole, context, timeout, log)

  override val cluster = Cluster(context.system)

  def receive: Receive = chatMsgReceive orElse clusterEventReceive orElse terminationReceive

  override def onMemberUp(member: Member): Unit = chatMsgPublishers.onMemberUp(member)
  override def onMemberDown(member: Member): Unit = chatMsgPublishers.onMemberDown(member)
  override def onTerminated(actor: ActorRef): Unit = chatMsgPublishers.onTerminated(actor)
}

object ChatMsgPublisherCluster extends ChatCluster[ChatMsgPublisherCluster](ChatMsgPublisherRole)
