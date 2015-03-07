name := """akka-chat"""

version := "1.0"

scalaVersion := "2.11.5"

val akkaVersion = "2.3.9"
val akkaStreamHttpVersion = "1.0-M4"

libraryDependencies ++= Seq(
  Library.akkaActor,
  Library.akkaRemote,
  Library.akkaPersistence,
  Library.akkaTestkit     % "test",
  Library.scalaTest       % "test",
  Library.scalaCheck      % "test",
  "com.github.krasserm" %% "akka-persistence-kafka" % "0.3.4",
  "com.typesafe.akka" %% "akka-http-core-experimental" % akkaStreamHttpVersion,
  "com.typesafe.akka" %% "akka-http-experimental" % akkaStreamHttpVersion,
  "com.typesafe.akka" %% "akka-http-java-jackson-experimental" % akkaStreamHttpVersion,
  "com.typesafe.akka" %% "akka-http-spray-json-experimental" % akkaStreamHttpVersion,
  "com.typesafe.akka" %% "akka-stream-experimental" % akkaStreamHttpVersion,
  "com.typesafe.akka" %% "akka-stream-testkit-experimental" % akkaStreamHttpVersion,
  "com.sclasen" %% "akka-kafka" % "0.1.0",
  "junit" % "junit" % "4.12" % "test"
//  "com.novocode" % "junit-interface" % "0.10" % "test"
)

testOptions += Tests.Argument(TestFrameworks.JUnit, "-v")

resolvers ++= List(
  Resolver.krasserm
//  Resolver.hseeberger,
//  Resolver.patriknw
)

EclipseKeys.withSource := true
