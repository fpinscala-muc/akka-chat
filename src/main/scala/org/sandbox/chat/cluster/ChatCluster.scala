package org.sandbox.chat.cluster

import akka.actor.ActorSystem
import com.typesafe.config.ConfigFactory
import akka.actor.Props
import scala.reflect.ClassTag
import akka.actor.Actor

class ChatCluster[T <: Actor: ClassTag](role: ChatClusterRole) {

  val clusterName = "akka-chat-cluster"

  def main(args: Array[String]): Unit = {
    // Override the configuration of the port when specified as program argument
    val port = if (args.isEmpty) "0" else args(0)
    val config = ConfigFactory.parseString(s"akka.remote.netty.tcp.port=$port")
      .withFallback(ConfigFactory.parseString(s"akka.cluster.roles = [${role.name}]"))
      .withFallback(ConfigFactory.load())

    val system = ActorSystem(clusterName, config)
    system.actorOf(Props[T], name = role.name)
  }
}
