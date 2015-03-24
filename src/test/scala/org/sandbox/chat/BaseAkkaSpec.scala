package org.sandbox.chat

import org.scalatest.BeforeAndAfterAll
import org.scalatest.FlatSpec

import akka.actor.ActorSystem
import akka.stream.ActorFlowMaterializer

abstract class BaseAkkaSpec extends FlatSpec with BeforeAndAfterAll {

  implicit val system = ActorSystem()
  implicit val ec = system.dispatcher
  implicit val mat = ActorFlowMaterializer()

  val settings = Settings(system)

  override protected def afterAll() = {
    system.shutdown()
    system.awaitTermination()
    super.afterAll()
  }
}
