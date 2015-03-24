package org.sandbox.chat

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt

import org.junit.runner.RunWith
import org.scalatest.BeforeAndAfterAll
import org.scalatest.BeforeAndAfterEach
import org.scalatest.FlatSpecLike
import org.scalatest.concurrent.Eventually
import org.scalatest.prop.PropertyChecks

import com.typesafe.config.ConfigFactory

import ChatServer.Ack
import ChatServer.Broadcast
import ChatServer.Contribution
import ChatServer.Join
import ChatServer.Leave
import ChatServer.Participant
import akka.actor.Actor
import akka.actor.ActorRef
import akka.actor.ActorSystem
import akka.actor.Props
import akka.actor.actorRef2Scala
import akka.pattern.ask
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

  override def beforeEach = {
    server = system.actorOf(ChatServer.props(), uniqueName("testServer"))
  }

  override def afterEach =
    system.stop(server)

  private def join(name: String, who: ActorRef = testActor) = {
    val join = Join(Participant(who, name))
    server ! join
    expectMsg(Ack(join))
  }

  private def leave(name: String) = {
    val leave = Leave(Participant(testActor, name))
    server ! leave
    expectMsg(Ack(leave))
  }

  private def contribution(name: String, msg: String, who: ActorRef = testActor, withAck: Boolean = true) = {
    val contrib = Contribution(Participant(who, name), msg)
    if (withAck) {
      // ask instead of expectMsg to not interfere w/ Broadcast messages
      val future = ask(server, contrib).mapTo[Ack]
      val ack = Await.result(future, 500 millis)
      assert(ack == Ack(contrib))
    }
    else server ! contrib
  }

  behavior of "ChatServer"

  it should "broadcast an incoming single Broadcast message to a joined actor" in {
    join("testClient")
    contribution("testClient", "bollocks")
    val Broadcast("testClient", "bollocks", _) = expectMsgType[Broadcast]
  }

  it should "not broadcast incoming Broadcast messages to a left actor" in {
    join("testClient")
    leave("testClient")
    contribution("testClient", "bollocks", testActor, false)
    expectNoMsg
  }

  it should "broadcast incoming Broadcast messages to a joined actor" in new MessageCollecting {
    join("testClient")
    contribution("testClient", "bollocks1")
    contribution("testClient", "bollocks2")
    receiveWhile(1 second) {
      case msg: Broadcast => collect(msg)
    }
    assert(messagesToPairs ==
      Seq(("testClient", "bollocks1"), ("testClient", "bollocks2")))
  }

  it should "broadcast incoming Broadcast messages to multiple joined members" in {
    val client1 = messageCollector
    join("client1", client1)
    val client2 = messageCollector
    join("client2", client2)

    contribution("client1", "bollocks1", client1)
    contribution("client2", "bollocks2", client2)
    val expectedMessages =
      Seq(("client1", "bollocks1"), ("client2", "bollocks2"))
    eventually {
      assert(client1.underlyingActor.messagesToPairs == expectedMessages)
      assert(client2.underlyingActor.messagesToPairs == expectedMessages)
    }
  }

  it should "broadcast random incoming Broadcast messages" in new MessageCollecting {
    val client = messageCollector
    join("client", client)
    forAll("msg") { msg: String =>
      contribution("client", msg, client)
      val broadcastMsg = Broadcast("client", msg)
      collect(broadcastMsg)
      eventually {
        assert(client.underlyingActor.messageToPair == messageToPair)
      }
    }
    assert(client.underlyingActor.messagesToPairs == messagesToPairs)
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
    private def broadcastToPair(b: Broadcast) = (b.authorName, b.msg)
    def messagesToPairs = messages map broadcastToPair
    def messageToPair = broadcastToPair(message)
  }

  class MessageCollector extends Actor with MessageCollecting {
    def receive: Actor.Receive = {
      case msg: Broadcast => collect(msg)
    }
  }
  def messageCollector(implicit system: ActorSystem) =
    TestActorRef[MessageCollector](Props[MessageCollector])
}
