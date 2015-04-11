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

  def receive: Receive =
    receiveAndThen(update, distribute) orElse query orElse
    clusterEventReceive orElse terminationReceive

//  private val updateAndDistribute: Receive = {
//    case msg if update.isDefinedAt(msg) =>
//      update(msg)
//      distribute(msg)
//  }

  private def receiveAndThen(receive: Receive, f: Any => Unit): Receive = {
    case msg if receive.isDefinedAt(msg) =>
      receive(msg)
      f(msg)
  }

  private def distribute(msg: Any): Unit =
    participantAdmins foreach { case (_, actor) => actor ! msg }

  override def onMemberUp(member: Member): Unit = participantAdmins.onMemberUp(member)
  override def onMemberDown(member: Member): Unit = participantAdmins.onMemberDown(member)
  override def onTerminated(actor: ActorRef): Unit = participantAdmins.onTerminated(actor)
}

object ParticipantAdministratorCluster extends ChatCluster[ParticipantAdministratorCluster](ParticipantAdministratorRole)
