package org.sandbox.chat

import scala.concurrent.duration.DurationInt

import org.junit.runner.RunWith
import org.scalatest.BeforeAndAfterAll
import org.scalatest.BeforeAndAfterEach
import org.scalatest.Finders
import org.scalatest.FlatSpecLike
import org.scalatest.concurrent.Eventually

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
  with FlatSpecLike with BeforeAndAfterAll with BeforeAndAfterEach
  with Eventually {
  override def afterAll = shutdown()

  import ChatServer._

  var server: ActorRef = _

  override def beforeEach =
    server = system.actorOf(Props[ChatServer], ChatServerSpec.uniqueName("testServer"))

  override def afterEach =
    system.stop(server)

  behavior of "ChatServer"

  it should "broadcast an incoming single Broadcast message to a joined actor" in {
    server ! Join("testClient")
    server ! Broadcast("bollocks")
    expectMsg(Broadcast("testClient: bollocks"))
  }

  it should "not broadcast incoming Broadcast messages to a left actor" in {
    server ! Join("testClient")
    server ! Leave
    server ! Broadcast("bollocks")
    expectNoMsg
  }

  trait MessageCollector {
    var messages = Seq.empty[Broadcast]
    def collect(msg: Broadcast) = messages = messages ++ Seq(msg)
  }

  it should "broadcast incoming Broadcast messages to a joined actor" in new MessageCollector {
    server ! Join("testClient")
    server ! Broadcast("bollocks1")
    server ! Broadcast("bollocks2")
    receiveWhile(1 second) {
      case msg: Broadcast => collect(msg)
    }
    assert(messages ==
      Seq(Broadcast("testClient: bollocks1"), Broadcast("testClient: bollocks2")))
  }

  it should "broadcast incoming Broadcast messages to multiple joined members" in {
    class TestClient(name: String) extends Actor with MessageCollector {
      def receive: Actor.Receive = {
        case msg: Broadcast => collect(msg)
      }
    }
    def testClient(name: String) =
      TestActorRef[TestClient](Props(new TestClient(name)))

    val client1 = testClient("client1")
    server.tell(Join("client1"), client1)
    val client2 = testClient("client2")
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
}

object ChatServerSpec {
  val configStr = """
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
}
