package org.sandbox.chat

import ChatServer.Participant
import akka.actor.Actor
import akka.actor.Props

trait ParticipantAdministrating extends Actor {
  import ParticipantAdministrator._
  import ChatServer._

  var participants: Set[Participant] = Set.empty

  def participantReceive: Receive = update orElse query

  protected def update: Receive = {
    case join@Join(who) =>
      participants += who
      sender ! Ack(join)
    case leave@Leave(who) =>
      participants -= who
      sender ! Ack(leave)
  }

  protected def query: Receive = {
    case isJoined@IsJoined(who, _) =>
      sender ! isJoined.copy(joined = participants.contains(who))
    case GetAllParticipants =>
      sender ! AllParticipants(participants)
  }
}
