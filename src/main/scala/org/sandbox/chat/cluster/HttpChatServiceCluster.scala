package org.sandbox.chat.cluster

import scala.concurrent.duration.DurationInt
import org.sandbox.chat.ParticipantAdministrating
import akka.cluster.Member
import akka.util.Timeout
import akka.actor.ActorRef
import akka.cluster.Cluster
import org.sandbox.chat.http.HttpChatServing
import akka.http.model.HttpResponse
import org.sandbox.chat.Settings
import org.sandbox.chat.http.HttpResponseChatServiceActions

class HttpChatServiceCluster extends HttpChatServing with ClusterEventReceiver {
  thisCluster =>

  override implicit val timeout = Timeout(1 second)

  val chatServers =
    new ChatClusterActors(ChatServiceRole, context, timeout, log)
  def chatServer: ActorRef =
    chatServers.randomActor getOrElse(throw new Exception("no chatServers available"))

  override val chatServiceActions =
    new HttpResponseChatServiceActions {
      override val system = context.system
      override def chatServer = thisCluster.chatServer
  }

  override val cluster = Cluster(context.system)

  val httpChatServices =
    new ChatClusterActors(HttpChatServiceRole, context, timeout, log)

  def receive: Receive =
    startServiceReceive orElse serviceReceive orElse
    clusterEventReceive orElse terminationReceive

  override def onMemberUp(member: Member): Unit = {
    chatServers.onMemberUp(member)
    httpChatServices.onMemberUp(member)
  }
  override def onMemberDown(member: Member): Unit = {
    chatServers.onMemberDown(member)
    httpChatServices.onMemberDown(member)
  }
  override def onTerminated(actor: ActorRef): Unit = {
    chatServers.onTerminated(actor)
    httpChatServices.onTerminated(actor)
  }
}

object HttpChatServiceCluster extends ChatCluster[HttpChatServiceCluster](HttpChatServiceRole)
