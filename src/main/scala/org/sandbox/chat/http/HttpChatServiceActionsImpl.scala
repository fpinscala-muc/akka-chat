package org.sandbox.chat.http

import akka.actor.ActorRef
import akka.actor.ActorSystem

class HttpChatServiceActionsImpl(override val chatServer: ActorRef, override val system: ActorSystem)
  extends HttpResponseChatServiceActions
