package org.sandbox.chat

import ChatServer.Participant
import akka.actor.Actor
import akka.actor.Props

class ParticipantAdministrator extends Actor {
  import ParticipantAdministrator._
  import ChatServer._

  var participants: Set[Participant] = Set.empty

  def receive: Receive = update orElse query

  private def update: Receive = {
    case join@Join(who) =>
      participants += who
      sender ! Ack(join)
    case leave@Leave(who) =>
      participants -= who
      sender ! Ack(leave)
  }

  private def query: Receive = {
    case isJoined@IsJoined(who, _) =>
      sender ! isJoined.copy(joined = participants.contains(who))
    case GetAllParticipants =>
      sender ! AllParticipants(participants)
  }
}

object ParticipantAdministrator {
  def props(): Props = Props[ParticipantAdministrator]

  sealed trait ParticipantAdministratorMsg
  case class IsJoined(who: Participant, joined: Boolean = false) extends ParticipantAdministratorMsg
  case object GetAllParticipants extends ParticipantAdministratorMsg
  case class AllParticipants(participants: Set[Participant]) extends ParticipantAdministratorMsg
}
