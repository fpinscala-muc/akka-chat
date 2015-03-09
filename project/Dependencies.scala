import sbt._

object Version {
  val akka                 = "2.3.9"
  val akkaDataReplication  = "0.10"
  val akkaHttp             = "1.0-M4"
  val akkaPersistenceMongo = "0.7.5"
  val akkaSse              = "0.5.0"
  val logback              = "1.1.2"
  val scala                = "2.11.5"
  val scalaMock            = "3.2.1"
  val scalaTest            = "2.2.3"
  val scalaCheck           = "1.12.1"
  val sprayJson            = "1.3.1"
}

object Library {
  val akkaActor            = "com.typesafe.akka"   %% "akka-actor"                     % Version.akka
  val akkaRemote           = "com.typesafe.akka"   %% "akka-remote"                    % Version.akka
  val akkaTestkit          = "com.typesafe.akka"   %% "akka-testkit"                   % Version.akka
  val akkaPersistence      = "com.typesafe.akka"   %% "akka-persistence-experimental"  % Version.akka
  val akkaCluster          = "com.typesafe.akka"   %% "akka-cluster"                   % Version.akka
  val akkaContrib          = "com.typesafe.akka"   %% "akka-contrib"                   % Version.akka
  val akkaDataReplication  = "com.github.patriknw" %% "akka-data-replication"          % Version.akkaDataReplication
  val akkaHttp             = "com.typesafe.akka"   %% "akka-http-experimental"         % Version.akkaHttp
  val akkaHttpTestkit      = "com.typesafe.akka"   %% "akka-http-testkit-experimental" % Version.akkaHttp
  val akkaPersistenceMongo = "com.github.ironfish" %% "akka-persistence-mongo-casbah"  % Version.akkaPersistenceMongo
  val akkaSlf4j            = "com.typesafe.akka"   %% "akka-slf4j"                     % Version.akka
  val akkaSse              = "de.heikoseeberger"   %% "akka-sse"                       % Version.akkaSse
  val logbackClassic       = "ch.qos.logback"      %  "logback-classic"                % Version.logback
  val scalaMock            = "org.scalamock"       %% "scalamock-scalatest-support"    % Version.scalaMock
  val scalaTest            = "org.scalatest"       %% "scalatest"                      % Version.scalaTest
  val scalaCheck           = "org.scalacheck"      %% "scalacheck"                     % Version.scalaCheck
  val sprayJson            = "io.spray"            %% "spray-json"                     % Version.sprayJson
}

object Resolver {
  val hseeberger = "hseeberger at bintray" at "http://dl.bintray.com/hseeberger/maven"
  val patriknw   = "patriknw at bintray"   at "http://dl.bintray.com/patriknw/maven"
  val krasserm = "krasserm at bintray" at "http://dl.bintray.com/krasserm/maven"
}
