package org.sandbox.chat

import akka.actor.Actor
import akka.actor.ExtendedActorSystem
import akka.actor.Extension
import akka.actor.ExtensionKey

object Settings extends ExtensionKey[Settings]


/** Configuration settings for akka-chat. */
class Settings(system: ExtendedActorSystem) extends Extension {
  private val akkaChat = system.settings.config.getConfig("akka-chat")

  object httpService {
    val interface = akkaChat.getString("http-service.interface")
    val port = akkaChat.getInt("http-service.port")
  }

  object sseService {
    val interface = akkaChat.getString("sse-service.interface")
    val port = akkaChat.getInt("sse-service.port")
  }
}

/** Convenient access to configuration settings for actors. */
trait SettingsActor {
  this: Actor =>

  val settings = Settings(context.system)
}
