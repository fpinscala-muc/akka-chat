package org.sandbox.chat.http

import scala.concurrent.duration.DurationInt
import org.sandbox.chat.ChatServer.Broadcast
import org.sandbox.chat.ChatServer.Join
import org.sandbox.chat.ChatServer.Leave
import HttpChatClient.{GetBroadcasts, Broadcasts}
import akka.actor.ActorRef
import akka.actor.ActorSystem
import akka.actor.actorRef2Scala
import akka.http.model.HttpEntity.apply
import akka.http.model.HttpResponse
import akka.http.model.StatusCodes.NotFound
import akka.http.model.StatusCodes.OK
import akka.pattern.ask
import akka.util.Timeout
import scala.concurrent.Await

class HttpChatServerActions(chatServer: ActorRef, system: ActorSystem) {

  import system.dispatcher
  implicit val timeout = Timeout(1 second)

  var chatClients: Map[String,ActorRef] = Map.empty

  private def chatterNames = chatClients.keySet.toSeq.sorted

  private def ok(msg: String) = HttpResponse(OK, entity = s"$msg\n")
  private def notFound(name: String) = HttpResponse(NotFound, entity = s"not found: $name\n")
  private def forChatClient(name: String)(f: ActorRef => HttpResponse) =
    chatClients.get(name) map f getOrElse notFound(name)

  def onJoin(name: String) = {
    val chatClient =
      system.actorOf(HttpChatClient.props(chatServer), s"httpClient-$name")
    chatClients += name -> chatClient
    chatClient ! Join(name)
    ok(s"joined: $name")
  }
  def onLeave(name: String) = {
    forChatClient(name) { chatClient =>
      chatClient ! Leave
      chatClients -= name
      ok(s"left: $name")
    }
  }
  def onBroadcast(name: String, msg: String) = {
    forChatClient(name) { chatClient =>
      chatClient ! Broadcast(msg)
      ok(s"broadcasted: $msg")
    }
  }
  def onPoll(name: String) = {
    forChatClient(name) { chatClient =>
      val messagesF = ask(chatClient, GetBroadcasts).mapTo[Broadcasts]
      val Broadcasts(messages) = Await.result(messagesF, timeout.duration)
      ok(s"${messages.mkString("\n")}")
    }
  }
  def onShutdown = {
    system.scheduler.scheduleOnce(500 millis)(system.shutdown)
    ok(s"shutdown: ${system.name} (chatters: ${chatterNames.mkString(",")})")
  }

}
