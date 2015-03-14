package org.sandbox.chat.http

import akka.http.model.HttpResponse

trait ChatServerActions {
  def onJoin(name: String): HttpResponse
  def onLeave(name: String): HttpResponse
  def onContribution(name: String, msg: String): HttpResponse
  def onPoll(name: String): HttpResponse
  def onShutdown: HttpResponse
}