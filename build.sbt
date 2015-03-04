name := """akka-chat"""

version := "1.0"

scalaVersion := "2.11.5"

val akkaVersion = "2.3.9"
val akkaStreamHttpVersion = "1.0-M3"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % akkaVersion,
  "com.typesafe.akka" %% "akka-remote" % akkaVersion,
  "com.typesafe.akka" %% "akka-testkit" % akkaVersion,
  "com.typesafe.akka" %% "akka-persistence-experimental" % akkaVersion,
  "com.github.krasserm" %% "akka-persistence-kafka" % "0.3.4",
  "com.typesafe.akka" %% "akka-http-core-experimental" % akkaStreamHttpVersion,
  "com.typesafe.akka" %% "akka-http-experimental" % akkaStreamHttpVersion,
  "com.typesafe.akka" %% "akka-http-java-jackson-experimental" % akkaStreamHttpVersion,
  "com.typesafe.akka" %% "akka-http-spray-json-experimental" % akkaStreamHttpVersion,
  "com.typesafe.akka" %% "akka-stream-experimental" % akkaStreamHttpVersion,
  "com.typesafe.akka" %% "akka-stream-testkit-experimental" % akkaStreamHttpVersion,
  "com.sclasen" %% "akka-kafka" % "0.1.0",
  "org.scalatest" %% "scalatest" % "2.2.3" % "test",
  "org.scalacheck" %% "scalacheck" % "1.12.1" % "test",
  "junit" % "junit" % "4.12" % "test"
//  "com.novocode" % "junit-interface" % "0.10" % "test"
)

testOptions += Tests.Argument(TestFrameworks.JUnit, "-v")

resolvers += "krasserm at bintray" at "http://dl.bintray.com/krasserm/maven"

EclipseKeys.withSource := true
