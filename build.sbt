name := """akka-chat"""

version := "1.0"

scalaVersion := "2.11.6"


libraryDependencies ++= Seq(
  Library.akkaActor,
  Library.akkaCluster,
  Library.akkaContrib,
  Library.akkaDataReplication,
  Library.akkaRemote,
  Library.akkaPersistence,
  Library.akkaSse,
  Library.akkaSlf4j,
  Library.logbackClassic,
  Library.akkaTestkit     % "test",
  Library.akkaHttpTestkit % "test",
  Library.scalaTest       % "test",
  Library.scalaMock       % "test",
  Library.scalaCheck      % "test",
  Library.akkaHttpCore,
  Library.akkaHttp,
  Library.akkaHttpJackson,
  Library.akkaHttpSpray,
  Library.akkaStream,
  Library.akkaStreamTestkit % "test",
  "com.github.krasserm" %% "akka-persistence-kafka" % "0.3.4",
  "com.sclasen" %% "akka-kafka" % "0.1.0",
  "junit" % "junit" % "4.12" % "test"
//  "com.novocode" % "junit-interface" % "0.10" % "test"
)

//addCommandAlias("jCartman", "run curl -X GET http://localhost:8080/join/Cartman")

testOptions += Tests.Argument(TestFrameworks.JUnit, "-v")

resolvers ++= List(
  Resolver.krasserm,
//  Resolver.hseeberger,
  Resolver.patriknw
)

EclipseKeys.withSource := true
