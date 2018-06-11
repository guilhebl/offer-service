import sbt.Keys._

lazy val GatlingTest = config("gatling") extend Test

name := "offer-service"

version := "1.0-SNAPSHOT"

scalaVersion in ThisBuild := "2.12.4"

crossScalaVersions := Seq("2.11.12", "2.12.4")

def gatlingVersion(scalaBinVer: String): String = scalaBinVer match {
  case "2.11" => "2.2.5"
  case "2.12" => "2.3.0"
}

libraryDependencies ++= Seq(
  ws,
  guice
)

libraryDependencies += "org.joda" % "joda-convert" % "1.9.2"
libraryDependencies += "net.logstash.logback" % "logstash-logback-encoder" % "4.11"
libraryDependencies += "com.netaporter" %% "scala-uri" % "0.4.16"
libraryDependencies += "net.codingwell" %% "scala-guice" % "4.1.1"
libraryDependencies += "org.mongodb.scala" %% "mongo-scala-driver" % "2.3.0"
libraryDependencies += "net.debasishg" %% "redisclient" % "3.7"

// test dependencies
libraryDependencies += "org.scalatestplus.play" %% "scalatestplus-play" % "3.1.2" % Test
libraryDependencies += "io.gatling.highcharts" % "gatling-charts-highcharts" % gatlingVersion(scalaBinaryVersion.value) % Test
libraryDependencies += "io.gatling" % "gatling-test-framework" % gatlingVersion(scalaBinaryVersion.value) % Test
libraryDependencies += "org.scalacheck" %% "scalacheck" % "1.14.0" % "test"
libraryDependencies += "de.leanovate.play-mockws" %% "play-mockws" % "2.6.0" % Test
libraryDependencies += "org.mockito" % "mockito-all" % "1.9.5" % Test
libraryDependencies += specs2 % Test

// libraryDependencies += "mysql" % "mysql-connector-java" % "5.1.41"
libraryDependencies += "commons-codec" % "commons-codec" % "1.3"

// The Play project itself
lazy val root = (project in file("."))
  .enablePlugins(Common, PlayScala, GatlingPlugin)
  .configs(GatlingTest)
  .settings(inConfig(GatlingTest)(Defaults.testSettings): _*)
  .settings(
    name := """offer-service""",
    scalaSource in GatlingTest := baseDirectory.value / "/gatling/simulation"
  )

// Documentation for this project:
//    sbt "project docs" "~ paradox"
//    open docs/target/paradox/site/index.html
lazy val docs = (project in file("docs")).enablePlugins(ParadoxPlugin).
  settings(
    paradoxProperties += ("download_url" -> "https://example.lightbend.com/v1/download/play-rest-api")
  )

javaOptions in Universal ++= Seq(
  // JVM memory tuning
  "-J-Xmx4096m",
  "-J-Xms1024m",

  // Since play uses separate pidfile we have to provide it with a proper path
  // name of the pid file must be play.pid
  // s"-Dpidfile.path=/opt/docker/${packageName.value}/run/play.pid"
  //s"-Dpidfile.path=/dev/null"
)

//dockerBaseImage := "cogniteev/oracle-java:java8"

// use ++= to merge a sequence with an existing sequence
////dockerCommands ++= Seq(
//  ExecCmd("RUN", "mkdir", s"/opt/docker/${packageName.value}"),
//  ExecCmd("RUN", "mkdir", s"/opt/docker/${packageName.value}/run"),
//  ExecCmd("RUN", "chown", "-R", "daemon:daemon", s"/opt/docker/${packageName.value}/")
//)

// exposing the play ports
//dockerExposedPorts in Docker := Seq(9000, 9443)