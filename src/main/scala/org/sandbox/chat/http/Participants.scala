package org.sandbox.chat.http

import scala.annotation.migration
import scala.concurrent.Await
import scala.concurrent.duration.DurationInt
import scala.reflect.ClassTag

import org.sandbox.chat.ChatServer.Ack
import org.sandbox.chat.ChatServer.Participant
import org.sandbox.chat.ChatServer.Shutdown
import org.sandbox.chat.http.HttpChatClient.Broadcasts
import org.sandbox.chat.http.HttpChatClient.GetBroadcasts

import akka.actor.ActorRef
import akka.actor.ActorSystem
import akka.pattern.ask
import akka.util.Timeout

trait Participants[T] {

  val system: ActorSystem
  def chatServer: ActorRef

  implicit val timeout = Timeout(1 second)

  var participants: Set[Participant] = Set.empty

  def addParticipant(participant: Participant) = participants += participant
  def removeParticipant(participant: Participant) = participants -= participant

  def notFound(name: String): T

  def forParticipant(name: String)(f: Participant => T) =
    participants.find(_.name == name) map f getOrElse notFound(name)

  def createParticipant(name: String): Participant = {
    val chatClient =
      system.actorOf(HttpChatClient.props(chatServer), s"httpClient-$name")
    Participant(chatClient, name)
  }

  def participantNames = (participants map(_.name)).toSeq.sorted

  def askFor[T: ClassTag](who: ActorRef, msg: Any): T = {
    val future = ask(who, msg).mapTo[T]
    Await.result(future, timeout.duration)
  }

  def askForBroadcasts(participant: Participant): Broadcasts =
    askFor[Broadcasts](participant.who, GetBroadcasts)

  def askForShutdown = {
    val Ack(Shutdown(participants)) =
      Await.result((chatServer ? Shutdown()).mapTo[Ack], timeout.duration)
    (participants map(_.name)).toSeq.sorted
  }
}
