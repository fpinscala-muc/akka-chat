package org.sandbox.chat.http

import akka.http.marshalling.ToResponseMarshallable
import akka.http.server.Directive.addByNameNullaryApply
import akka.http.server.Directive.addDirectiveApply
import akka.http.server.Directives.complete
import akka.http.server.Directives.enhanceRouteWithConcatenation
import akka.http.server.Directives.path
import akka.http.server.Directives.segmentStringToPathMatcher
import akka.http.server.PathMatcher.regex2PathMatcher
import akka.http.server.Route

class ChatRoutes[T <% ToResponseMarshallable] private(chatServerActions: ChatServerActions[T])
{
  import chatServerActions._
  import ChatRoutes.StringMatcher

  private def forName(f: String => T)(name: String) = complete(f(name))

  private val join = path("join" / StringMatcher)(forName(onJoin))
  private val leave = path("leave" / StringMatcher)(forName(onLeave))
  private val poll = path("poll" / StringMatcher)(forName(onPoll))
  private val contribution = path(("broadcast" | "contrib") / StringMatcher / StringMatcher) { (name, msg) =>
    complete(onContribution(name, msg))
  }
  private val shutdown = path("shutdown" / "shutdown") { complete(onShutdown) }

  val routes: Route =
    join ~ leave ~ contribution ~ poll ~ shutdown
}

object ChatRoutes {
  private val StringMatcher = "(.+)".r

  def apply[T <% ToResponseMarshallable](chatServerActions: ChatServerActions[T]): Route =
    new ChatRoutes(chatServerActions).routes
}