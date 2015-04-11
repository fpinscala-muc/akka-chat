package org.sandbox.chat.cluster

import scala.reflect.ClassTag
import com.typesafe.config.ConfigFactory
import akka.actor.Actor
import akka.actor.ActorSystem
import akka.actor.Props
import akka.cluster.Cluster
import com.typesafe.config.Config

class ChatCluster[T <: Actor: ClassTag](role: ChatClusterRole) {

  val clusterName = "akka-chat-cluster"

  def main(args: Array[String]): Unit = {
    // Override the configuration of the port when specified as program argument
    val port = if (args.isEmpty) "0" else args(0)
    val config = ChatCluster.getClusterConfig(port.toInt, role)

    val system = ActorSystem(clusterName, config)
    Cluster(system) registerOnMemberUp {
      val clusterActor = system.actorOf(Props[T], name = role.name)
      system.actorOf(ChatClusterReaper.props(clusterActor), name = ClusterReaperRole.name)
    }
  }
}

object ChatCluster {
  private val defaultConfig = ConfigFactory.load()
  private val clusterConfig = defaultConfig.getConfig("with-cluster")

  def getClusterConfig(port: Int, role: ChatClusterRole): Config = {
    ConfigFactory.parseString(s"akka.remote.netty.tcp.port=$port")
      .withFallback(ConfigFactory.parseString(
          s"akka.cluster.roles = [${role.name},${ClusterReaperRole.name}]"))
      .withFallback(clusterConfig)
      .withFallback(defaultConfig)
  }
}
