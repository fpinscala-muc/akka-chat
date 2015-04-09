package org.sandbox.chat.sse

import de.heikoseeberger.akkasse.ServerSentEvent
import org.sandbox.chat.ChatServer._
import spray.json._

object ChatMsgJsonProtocol extends DefaultJsonProtocol {
  implicit object ChatMsgFormat extends RootJsonFormat[ChatServerMsg] {
    def write(msg: ChatServerMsg) = msg match {
      case Join(who) => JsObject("name" -> JsString(who.name))
      case Leave(who) => JsObject("name" -> JsString(who.name))
      case Contribution(author, message) => JsObject("name" -> JsString(author.name), "msg" -> JsString(message))
      case Broadcast(name, message, when) =>
        JsObject("name" -> JsString(name), "msg" -> JsString(message), "date" -> JsNumber(when.getTime))
      case _ => throw new SerializationException("not supported yet")
    }

    def read(value: JsValue) = {
      throw new DeserializationException("not supported")
    }
  }
}

object SseConversions {
  implicit def chatServerMsgToSse(message: ChatServerMsg): ServerSentEvent = {
    import ChatMsgJsonProtocol._
    val event = message.getClass.getSimpleName.toLowerCase
    message match {
      case m => ServerSentEvent(m.toJson.compactPrint, event)
    }
  }
}
