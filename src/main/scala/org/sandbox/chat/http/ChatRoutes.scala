package org.sandbox.chat.http

import scala.annotation.implicitNotFound
import scala.concurrent.ExecutionContext

import akka.http.marshalling.ToResponseMarshallable
import akka.http.server.Directive.addByNameNullaryApply
import akka.http.server.Directive.addDirectiveApply
import akka.http.server.Directives.as
import akka.http.server.Directives.complete
import akka.http.server.Directives.enhanceRouteWithConcatenation
import akka.http.server.Directives.entity
import akka.http.server.Directives.path
import akka.http.server.Directives.post
import akka.http.server.Directives.put
import akka.http.server.Directives.segmentStringToPathMatcher
import akka.http.server.PathMatcher.regex2PathMatcher
import akka.http.server.Route
import akka.http.unmarshalling.FromRequestUnmarshaller
import akka.http.unmarshalling.Unmarshaller
import akka.stream.FlowMaterializer
import akka.util.ByteString

class ChatRoutes[T <% ToResponseMarshallable] private(chatServerActions: ChatServerActions[T])
    (implicit ec: ExecutionContext, mat: FlowMaterializer)
{
  import chatServerActions._
  import ChatRoutes.StringMatcher

  implicit val stringUnmarshaller: FromRequestUnmarshaller[String] =
    Unmarshaller { req =>
      val postDataF = req.entity.dataBytes.runFold(ByteString("")) { (z, i) => z.concat(i)}
      postDataF map(_.decodeString("UTF-8"))
    }

  private def forName(f: String => T)(name: String) = complete(f(name))

  private val join = path("join" / StringMatcher)(forName(onJoin))
  private val leave = path("leave" / StringMatcher)(forName(onLeave))
  private val poll = path("poll" / StringMatcher)(forName(onPoll))
  private val contribution =
    path(("broadcast" | "contrib") / StringMatcher) { name =>
      (post | put) {
        entity(as[String]) { msg =>
          complete(onContribution(name, msg))
        }
      }
    }
  private val shutdown = path("shutdown" / "shutdown") { complete(onShutdown) }

  val routes: Route =
    join ~ leave ~ contribution ~ poll ~ shutdown
}

object ChatRoutes {
  private val StringMatcher = "(.+)".r

  def apply[T <% ToResponseMarshallable](chatServerActions: ChatServerActions[T])
    (implicit ec: ExecutionContext, mat: FlowMaterializer): Route =
      new ChatRoutes(chatServerActions).routes
}