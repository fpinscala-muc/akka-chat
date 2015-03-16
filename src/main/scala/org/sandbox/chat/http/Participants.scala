package org.sandbox.chat.http

import org.sandbox.chat.ChatServer.Participant
import akka.actor.ActorSystem
import akka.actor.ActorRef

trait Participants[T] {

  val system: ActorSystem
  val chatServer: ActorRef

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
}
