package org.sandbox.chat.http

import akka.http.marshalling.ToResponseMarshallable

abstract class ChatServerActions[T <% ToResponseMarshallable] {
  def onJoin(name: String): T
  def onLeave(name: String): T
  def onContribution(name: String, msg: String): T
  def onPoll(name: String): T
  def onShutdown: T
}
