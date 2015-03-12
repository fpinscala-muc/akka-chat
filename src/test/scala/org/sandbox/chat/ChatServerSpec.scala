package org.sandbox.chat

import scala.concurrent.duration.DurationInt

import org.junit.runner.RunWith
import org.scalatest.BeforeAndAfterAll
import org.scalatest.BeforeAndAfterEach
import org.scalatest.Finders
import org.scalatest.FlatSpecLike
import org.scalatest.concurrent.Eventually
import org.scalatest.prop.PropertyChecks

import com.typesafe.config.ConfigFactory

import ChatServer.Broadcast
import ChatServer.Join
import ChatServer.Leave
import akka.actor.Actor
import akka.actor.ActorRef
import akka.actor.ActorSystem
import akka.actor.Props
import akka.actor.actorRef2Scala
import akka.testkit.DefaultTimeout
import akka.testkit.ImplicitSender
import akka.testkit.TestActorRef
import akka.testkit.TestKit

@RunWith(classOf[org.scalatest.junit.JUnitRunner])
class ChatServerSpec extends TestKit(ActorSystem("ChatServerSpec", ChatServerSpec.config))
  with ImplicitSender with DefaultTimeout
  with FlatSpecLike with PropertyChecks
  with BeforeAndAfterAll with BeforeAndAfterEach
  with Eventually
{
  override def afterAll = shutdown()

  import ChatServer._
  import ChatServerSpec._

  var server: ActorRef = _

  override def beforeEach =
    server = system.actorOf(Props[ChatServer], uniqueName("testServer"))

  override def afterEach =
    system.stop(server)

  private def join(name: String) = {
    server ! Join(name)
    expectMsg(Joined)
  }

  private def leave = {
    server ! Leave
    expectMsg(Left)
  }

  behavior of "ChatServer"

  it should "broadcast an incoming single Broadcast message to a joined actor" in {
    join("testClient")
    server ! Broadcast("bollocks")
    expectMsg(Broadcast("testClient: bollocks"))
  }

  it should "not broadcast incoming Broadcast messages to a left actor" in {
    join("testClient")
    leave
    server ! Broadcast("bollocks")
    expectNoMsg
  }

  it should "broadcast incoming Broadcast messages to a joined actor" in new MessageCollecting {
    join("testClient")
    server ! Broadcast("bollocks1")
    server ! Broadcast("bollocks2")
    receiveWhile(1 second) {
      case msg: Broadcast => collect(msg)
    }
    assert(messages ==
      Seq(Broadcast("testClient: bollocks1"), Broadcast("testClient: bollocks2")))
  }

  it should "broadcast incoming Broadcast messages to multiple joined members" in {
    val client1 = messageCollector("client1")
    server.tell(Join("client1"), client1)
    val client2 = messageCollector("client2")
    server.tell(Join("client2"), client2)

    server.tell(Broadcast("bollocks1"), client1)
    server.tell(Broadcast("bollocks2"), client2)
    val expectedMessages =
      Seq(Broadcast("client1: bollocks1"), Broadcast("client2: bollocks2"))
    eventually {
      assert(client1.underlyingActor.messages == expectedMessages)
      assert(client2.underlyingActor.messages == expectedMessages)
    }
  }

  it should "broadcast random incoming Broadcast messages" in new MessageCollecting {
    val client = messageCollector("client")
    server.tell(Join("client"), client)
    forAll("msg") { msg: String =>
      server.tell(Broadcast(msg), client)
      val broadcastMsg = Broadcast(s"client: $msg")
      collect(broadcastMsg)
      eventually {
        assert(client.underlyingActor.message == broadcastMsg)
      }
    }
    assert(client.underlyingActor.messages == messages)
//    println(messages.mkString("\n"))
  }
}

object ChatServerSpec {
  private[this] val configStr = """
akka {
  loglevel = "INFO"
}
"""
  val config = ConfigFactory.parseString(configStr).withFallback(ConfigFactory.load)

  private[this] var ints = Stream.from(1)
  def uniqueName(prefix: String) = {
    def nextInt = {
      val (i, s) = (ints.head, ints.tail)
      ints = s
      i
    }
    s"$prefix-$nextInt"
  }

  trait MessageCollecting {
    var messages = Seq.empty[Broadcast]
    var message: Broadcast = _
    def collect(broadcastMsg: Broadcast) = {
      message = broadcastMsg
      messages = messages :+ broadcastMsg
    }
  }

  class MessageCollector(name: String) extends Actor with MessageCollecting {
    def receive: Actor.Receive = {
      case msg: Broadcast => collect(msg)
    }
  }
  def messageCollector(name: String)(implicit system: ActorSystem) =
    TestActorRef[MessageCollector](Props(new MessageCollector(name)))

}
