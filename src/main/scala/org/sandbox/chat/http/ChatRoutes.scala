package org.sandbox.chat.http

import akka.http.marshalling.ToResponseMarshallable.apply
import akka.http.model.HttpResponse
import akka.http.server.Directive.addByNameNullaryApply
import akka.http.server.Directive.addDirectiveApply
import akka.http.server.Directives.complete
import akka.http.server.Directives.enhanceRouteWithConcatenation
import akka.http.server.Directives.path
import akka.http.server.Directives.segmentStringToPathMatcher
import akka.http.server.PathMatcher.regex2PathMatcher
import akka.http.server.Route

class ChatRoutes private(onJoin: String => HttpResponse, onLeave: String => HttpResponse,
    onBroadcast: (String,String) => HttpResponse, onPoll: String => HttpResponse,
    onShutdown: => HttpResponse)
{
  import ChatRoutes.StringMatcher

  val routes: Route =
    path("join" / StringMatcher) { name =>
      complete(onJoin(name))
    } ~
    path("leave" / StringMatcher) { name =>
      complete(onLeave(name))
    } ~
    path("broadcast" / StringMatcher / StringMatcher) { (name, msg) =>
      complete(onBroadcast(name, msg))
    } ~
    path("poll" / StringMatcher) { name =>
      complete(onPoll(name))
    } ~
    path("shutdown" / "shutdown") {
      complete(onShutdown)
    }
}

object ChatRoutes {
  private val StringMatcher = "(.+)".r

  def apply(onJoin: String => HttpResponse, onLeave: String => HttpResponse,
      onBroadcast: (String,String) => HttpResponse,
      onPoll: String => HttpResponse, onShutdown: => HttpResponse): Route =
    new ChatRoutes(onJoin, onLeave, onBroadcast, onPoll, onShutdown).routes
}