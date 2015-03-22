# akka-chat
A simple chat server using Akka and Scala.

## 1. First Version [Commit](https://github.com/fpinscala-muc/akka-chat/commit/082ba8b351403314ccc229882af45a708c06833e)
* actor ChatServer: Join and Leave the chat group, Broadcast an incoming message to all current chat members
* actor ChatClient: Join the chat group, Broadcast a first intro message
* ChatApp: first running App with three ChatClients

## 2. Second Version
* added support for [SSE](http://www.w3.org/TR/eventsource/) using Heiko Seeberger's [akka-sse](https://github.com/hseeberger/akka-sse) library.

## Try it out
Clone this repository, `cd` into it and start [sbt](http://www.scala-sbt.org). Then execute `run` and choose `org.sandbox.chat.http.HttpChatApp`:

```
akka-chat$ sbt
[info] ...
akka-chat$ run
[info] ...
Multiple main classes detected, select one to run:

 [1] org.sandbox.chat.http.HttpChatApp
 [2] org.sandbox.chat.ChatApp

Enter number: 1
[info] ...
```

Open a second console and start an [SSE](http://www.w3.org/TR/eventsource/) session on port `9000`:

```
~$ curl -X GET http://localhost:9000
```

Open a third console and send chat client commands to the server on port `8080`.
You will see the respective commands as SSEs sent to the second console:

```
~$ curl -X GET http://localhost:8080/join/Achim
~$ curl POST --data 'one line
another line' http://localhost:8080/contrib/Achim
~$ curl -X GET http://localhost:8080/join/Michael
~$ curl -X PUT --data 'one line again
and yet another line' http://localhost:8080/contrib/Michael
~$ curl -X GET http://localhost:8080/poll/Achim
~$ curl -X GET http://localhost:8080/leave/Michael
~$ curl -X GET http://localhost:8080/shutdown/shutdown
```

The [Akka HTTP](http://doc.akka.io/docs/akka-stream-and-http-experimental/1.0-M4/scala/http/index.html) endpoints are defined in [ChatRoutes](src/main/scala/org/sandbox/chat/http/ChatRoutes.scala).


![Alt text](https://snap-ci.com/fpinscala-muc/akka-chat/branch/master/build_image)
