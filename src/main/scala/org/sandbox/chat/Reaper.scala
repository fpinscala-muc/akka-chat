package org.sandbox.chat

import akka.actor.{ OneForOneStrategy, Actor, ActorLogging, ActorRef, Props, SupervisorStrategy, Terminated }

// shamelessly stolen from Heiko Seeberger's reactive-flows
object Reaper {

  /** Name for the [[Reaper]] actor. */
  final val Name = "reaper"

  /** Factory for [[Reaper]] `Props`. */
  def props = Props(new Reaper)
}

/** Single top-level actor. */
class Reaper extends Actor with ActorLogging with SettingsActor {

  /** Try to restart faulty child actors up to 3 times. */
  override val supervisorStrategy = OneForOneStrategy(3)(SupervisorStrategy.defaultDecider)

  val chatServer = context.actorOf(ChatServer.props(false), "ChuckNorris")
  context.watch(chatServer)

  /** Upon termination of a child actor shutdown the actor system. */
  override def receive = {
    case Terminated(actorRef) =>
      log.warning(s"Shutting down, because ${actorRef.path} has terminated!")
      shutdown()
  }

  /** Factory for the [[HttpService]] actor. */
//  protected def createHttpService() = context.actorOf(
//    HttpService.props(
//      settings.httpService.interface,
//      settings.httpService.port,
//      settings.httpService.askSelfTimeout,
//      settings.httpService.flowRegistryTimeout,
//      settings.httpService.flowShardingTimeout
//    ),
//    HttpService.Name
//  )

  /** Shutdown the actor system. */
  protected def shutdown() = context.system.shutdown()
}
