package org.sandbox.chat

import ChatServer.Participant
import akka.actor.Props

trait ParticipantAdministrator extends ParticipantAdministrating {
  def receive = participantReceive
}

object ParticipantAdministrator {
  def props(): Props = Props[ParticipantAdministrator]

  sealed trait ParticipantAdministratorMsg
  case class IsJoined(who: Participant, joined: Boolean = false) extends ParticipantAdministratorMsg
  case object GetAllParticipants extends ParticipantAdministratorMsg
  case class AllParticipants(participants: Set[Participant]) extends ParticipantAdministratorMsg
}
