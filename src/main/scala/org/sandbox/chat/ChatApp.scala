package org.sandbox.chat

import akka.actor.ActorSystem
import akka.actor.Props
import akka.cluster.Cluster

object ChatApp extends App {

  val system = ActorSystem("akka-chat")
  val settings = Settings(system)

    system.log.debug("Waiting to become a cluster member ...")
    Cluster(system).registerOnMemberUp {
//      Sharding(system).start()
      system.actorOf(Reaper.props, Reaper.Name)
      system.log.info("Akka-Chat Cluster up and running")
    }

  system.log.info(s"ChatApp with ActorSystem ${system.name} started")
  system.registerOnTermination(system.log.info(s"ActorSystem ${system.name} shutting down ..."))

  system.awaitTermination
}
