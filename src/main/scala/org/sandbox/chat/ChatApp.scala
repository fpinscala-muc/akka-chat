package org.sandbox.chat

import akka.actor.ActorSystem
import akka.actor.Props

object ChatApp extends App {

  val system = ActorSystem("chat-app")

  val server =
    system.actorOf(Props[ChatServer], "ChuckNorris")

  val client1 =
    system.actorOf(Props(new ChatClient("client1", server)), "client1")
  val client2 =
    system.actorOf(Props(new ChatClient("client2", server)), "client2")
  val client3 =
    system.actorOf(Props(new ChatClient("client3", server)), "client3")

  system.shutdown
}
