package org.sandbox.chat.http

import akka.http.server.Route
import akka.http.server.Directives._
import akka.http.model.HttpResponse
import akka.http.model.StatusCodes._

object ChatRoutes {

  val route: Route =
    path("ping") {
      complete(HttpResponse(OK, entity = "PONG!"))
    } ~
    path("order" / IntNumber) { id =>
      get {
        complete {
//          "Received GET request for order " + id
          HttpResponse(OK, entity = "Received GET request for order " + id)
        }
      } ~
        put {
          complete {
//            "Received PUT request for order " + id
          HttpResponse(NotFound, entity = "Unfortunately, the resource couldn’t be found.")
          }
        }
    }
}
