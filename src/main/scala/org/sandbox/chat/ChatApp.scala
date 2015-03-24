package org.sandbox.chat

import akka.actor.ActorSystem
import akka.actor.Props

object ChatApp extends App {

  val system = ActorSystem("chat-app")

  val server =
    system.actorOf(ChatServer.props(), "ChuckNorris")

  val Seq(client1, client2, client3) =
    (1 to 3) map { i =>
      system.actorOf(ChatClient.props(s"client$i", server), s"client$i")
    }

  system.shutdown
}
