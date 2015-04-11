package org.sandbox.chat.cluster

import scala.concurrent.duration.DurationInt

import org.sandbox.chat.SettingsActor

import akka.actor.Actor
import akka.actor.ActorLogging
import akka.actor.ActorRef
import akka.actor.OneForOneStrategy
import akka.actor.Props
import akka.actor.SupervisorStrategy
import akka.actor.Terminated
import akka.actor.actorRef2Scala
import akka.cluster.Cluster
import akka.cluster.Member
import akka.util.Timeout

class ChatClusterReaper(watchee: ActorRef) extends Actor with ActorLogging with SettingsActor
   with ClusterEventReceiver
{

  import ChatClusterReaper._

  override val cluster = Cluster(context.system)

  override implicit val timeout = Timeout(1 second)

  val clusterReapers =
    new ChatClusterActors(ClusterReaperRole, context, timeout, log)

  /** Try to restart faulty child actors up to 3 times. */
  override val supervisorStrategy = OneForOneStrategy(3)(SupervisorStrategy.defaultDecider)

  context.watch(watchee)

  override def receive: Receive =
    clusterEventReceive orElse handleShutdown

  /** Upon termination of a child actor shutdown the actor system. */
  def handleShutdown: Receive = {
    case Terminated(actorRef) =>
      log.warning(s"Shutting down, because ${actorRef.path} has terminated!")
      shutdown
    case Shutdown =>
      clusterReapers foreach { case (_, reaper) => reaper ! Shutdown }
      shutdown
  }

  /** Shutdown the actor system. */
  protected def shutdown = context.system.shutdown()

  
  override def onMemberUp(member: Member): Unit = clusterReapers.onMemberUp(member)
  override def onMemberDown(member: Member): Unit = clusterReapers.onMemberDown(member)
  override def onTerminated(actor: ActorRef): Unit = clusterReapers.onTerminated(actor)
}

object ChatClusterReaper {

  def props(watchee: ActorRef) = Props(new ChatClusterReaper(watchee))

  sealed trait ChatClusterReaperMsg
  case object Shutdown extends ChatClusterReaperMsg
}
