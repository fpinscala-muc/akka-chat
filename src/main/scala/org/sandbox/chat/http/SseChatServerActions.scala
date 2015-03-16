package org.sandbox.chat.http

import akka.http.model.HttpResponse
import akka.stream.scaladsl.Source
import akka.stream.actor.ActorPublisher
import de.heikoseeberger.akkasse.EventStreamMarshalling
import de.heikoseeberger.akkasse.ServerSentEvent
import akka.http.server.Route
import akka.actor.ActorRef
import akka.actor.ActorSystem
import akka.http.server.Directives
import scala.concurrent.ExecutionContext
import akka.http.model.HttpEntity
import akka.http.marshalling.Marshaller
import akka.http.model.HttpCharsets
import akka.http.model.MediaType
import akka.util.ByteString
import akka.http.marshalling.ToResponseMarshallable
import scala.concurrent.Future

class SseChatServerActions(chatServer: ActorRef, system: ActorSystem) extends ChatServerActions[ToResponseMarshallable]
  with Directives with EventStreamMarshalling
{

  import org.sandbox.chat.ChatServer._
  import system.dispatcher

  implicit def chatServerMsgToServerSentEvent(msg: ChatServerMsg): ServerSentEvent = msg match {
    case m => ServerSentEvent(m.toString, "event")
  }

//  val textEventStream: MediaType = MediaType.custom("text", "event-stream")
//  implicit def toResponseMarshallerX[ChatServerMsg](implicit ec: ExecutionContext): akka.http.marshalling.ToResponseMarshaller[Source[ChatServerMsg, Unit]] =
//    Marshaller.withFixedCharset(textEventStream, HttpCharsets.`UTF-8`) { messages =>
//      HttpResponse(entity = HttpEntity.CloseDelimited(textEventStream, messages.map(s => ByteString(s.toString.getBytes))))
//    }
//  
  private def createChatServerMsgPublisher(): ActorRef = ???
  private def createSource =
    Source(ActorPublisher[ChatServerMsg](createChatServerMsgPublisher()))

  def onContribution(name: String, msg: String): ToResponseMarshallable = {
        createSource
  }

  def foo(x: akka.http.marshalling.ToResponseMarshallable) = complete(x)
  def test: Route =
    path("messages") {
    get {
      complete {
        createSource
      }
    }
  }

  def onJoin(name: String): ToResponseMarshallable = {
    val chatClient =
      system.actorOf(HttpChatClient.props(chatServer), s"httpClient-$name")
    val participant = Participant(chatClient, name)
    val msg = Join(participant)
    Source(Future.successful(msg))
  }

  def onLeave(name: String): ToResponseMarshallable = {
    ???
  }

  def onPoll(name: String): ToResponseMarshallable = {
    ???
  }

  def onShutdown: ToResponseMarshallable = {
    ???
  }
}
