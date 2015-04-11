package org.sandbox.chat.http

import scala.concurrent.Future

import org.junit.runner.RunWith
import org.sandbox.chat.BaseAkkaSpec
import org.scalatest.Ignore
import org.scalatest.concurrent.Eventually
import org.scalatest.concurrent.ScalaFutures

import akka.http.Http
import akka.http.model.ContentType.apply
import akka.http.model.HttpEntity
import akka.http.model.HttpMethods.GET
import akka.http.model.HttpMethods.PUT
import akka.http.model.HttpRequest
import akka.http.model.HttpResponse
import akka.http.model.MediaTypes._
import akka.http.model.StatusCodes
import akka.http.model.Uri.apply
import akka.http.unmarshalling.Unmarshal
import akka.stream.scaladsl.Sink
import akka.stream.scaladsl.Source
import akka.util.ByteString

@Ignore
@RunWith(classOf[org.scalatest.junit.JUnitRunner])
class HttpChatSpec extends BaseAkkaSpec with ScalaFutures with Eventually {

  // HttpChat is blocking, so it is started in a separate thread
  // TODO make this proper
  var httpChatReady = false
  val httpChat = Future(new HttpChat { override def onReady: Unit = httpChatReady = true})
  eventually(httpChatReady == true)

  // TODO proper shutdown
//  override def afterAll = {
//    httpChat.foreach (_.shutdown)
//    super.afterAll
//  }

  behavior of "HttpChatApp"

//  it should "reject contributions of unjoined users" in {
//    val req = HttpRequest(PUT, uri = "/contrib/Joker",
//                entity = HttpEntity(`text/plain`, ByteString("bollocks")))
//    val resp = sendRequest(req)
//    whenReady(resp) { response =>
//      assert(response.status == StatusCodes.NotFound)
//    }
//  }

  // TODO fix this: for some reason the first request always fails with a timeout
  it should "accept a join request" in {
    val req = HttpRequest(GET, uri = "/join/Batman")
    val resp = sendRequest(req)
    whenReady(resp) { response =>
      assert(response.status == StatusCodes.OK)
      val content = Unmarshal(response.entity).to[String]
      whenReady(content)(str => assert(str == "joined: Batman"))
    }
  }

//  it should "accept another join request" in {
//    val req = HttpRequest(GET, uri = "/join/Robin")
//    val resp = sendRequest(req)
//    whenReady(resp) { response =>
//      assert(response.status == StatusCodes.OK)
//      val content = Unmarshal(response.entity).to[String]
//      whenReady(content) {str => println(str) }
//    }
//  }

  it should "accept contributions of joined users" in {
    val req = HttpRequest(PUT, uri = "/contrib/Batman",
                entity = HttpEntity(`text/plain`, ByteString("bollocks")))
    val resp = sendRequest(req)
    whenReady(resp) { response =>
      assert(response.status == StatusCodes.OK)
      val content = Unmarshal(response.entity).to[String]
      whenReady(content)(str => assert(str == "contribution: bollocks\n"))
    }
  }

  it should "reject contributions of unjoined users" in {
    val req = HttpRequest(PUT, uri = "/contrib/Joker",
                entity = HttpEntity(`text/plain`, ByteString("garbage")))
    val resp = sendRequest(req)
    whenReady(resp) { response =>
      assert(response.status == StatusCodes.NotFound)
    }
  }

  it should "shutdown on shutdown/shutdown" in {
    val req = HttpRequest(GET, uri = "/shutdown/shutdown")
    val resp = sendRequest(req)
    whenReady(resp) { response =>
      assert(response.status == StatusCodes.OK)
      val content = Unmarshal(response.entity).to[String]
      whenReady(content)(_.startsWith("shutdown: "))
    }
  }

  def sendRequest(req: HttpRequest): Future[HttpResponse] =
    Source.single(req)
      .via(Http()
        .outgoingConnection(host = settings.httpService.interface,
                            port = settings.httpService.port))
        .runWith(Sink.head)
}
