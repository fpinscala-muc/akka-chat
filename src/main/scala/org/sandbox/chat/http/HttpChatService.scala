package org.sandbox.chat.http

import akka.actor.Props
import akka.http.model.HttpResponse

object HttpChatService {
  def props(chatServiceActions: HttpChatServiceActions[HttpResponse]): Props =
    Props(new HttpChatService(chatServiceActions))
}

class HttpChatService(override val chatServiceActions: HttpChatServiceActions[HttpResponse])
  extends HttpChatServing
{
  override def receive = startServiceReceive orElse serviceReceive
}
