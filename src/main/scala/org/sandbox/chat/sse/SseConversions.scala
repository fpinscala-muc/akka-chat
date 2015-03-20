package org.sandbox.chat.sse

import org.sandbox.chat.ChatServer.ChatServerMsg

import de.heikoseeberger.akkasse.ServerSentEvent

object SseConversions {
  implicit def chatServerMsgToSse(message: ChatServerMsg): ServerSentEvent = {
    val event = message.getClass.getSimpleName.toLowerCase
    message match {
      case m => ServerSentEvent(m.toString, event)
    }
  }
}
