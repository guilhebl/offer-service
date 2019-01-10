import sbt.Keys._

lazy val GatlingTest = config("gatling") extend Test

name := "offer-service"
version := "1.0-SNAPSHOT"

scalaVersion in ThisBuild := "2.12.7"

libraryDependencies += ws
libraryDependencies += guice

libraryDependencies += "org.joda" % "joda-convert" % "2.1.2"
libraryDependencies += "net.logstash.logback" % "logstash-logback-encoder" % "5.2"
libraryDependencies += "com.netaporter" %% "scala-uri" % "0.4.16"
libraryDependencies += "net.codingwell" %% "scala-guice" % "4.2.1"
libraryDependencies += "org.mongodb.scala" %% "mongo-scala-driver" % "2.5.0"
libraryDependencies += "net.debasishg" %% "redisclient" % "3.8"
libraryDependencies += "commons-codec" % "commons-codec" % "1.3"
libraryDependencies += "com.typesafe.play" %% "play-mailer" % "6.0.1"
libraryDependencies += "com.typesafe.play" %% "play-mailer-guice" % "6.0.1"

libraryDependencies += "org.scalatestplus.play" %% "scalatestplus-play" % "4.0.0-RC2" % Test
libraryDependencies += "io.gatling.highcharts" % "gatling-charts-highcharts" % "3.0.1.1" % Test
libraryDependencies += "io.gatling" % "gatling-test-framework" % "3.0.1.1" % Test
libraryDependencies += "org.scalacheck" %% "scalacheck" % "1.14.0" % "test"
libraryDependencies += "de.leanovate.play-mockws" %% "play-mockws" % "2.6.6" % Test
libraryDependencies += "org.mockito" % "mockito-all" % "1.10.19" % Test

libraryDependencies += specs2 % Test

// The Play project itself
lazy val root = (project in file("."))
  .enablePlugins(Common, PlayScala, GatlingPlugin)
  .configs(GatlingTest)
  .settings(inConfig(GatlingTest)(Defaults.testSettings): _*)
  .settings(
    name := """offer-service""",
    scalaSource in GatlingTest := baseDirectory.value / "/gatling/simulation"
  )

// JVM memory tuning
javaOptions in Universal ++= Seq(
  "-J-Xmx12056m",
  "-J-Xms1024m"
)

// Documentation for this project:
//    sbt "project docs" "~ paradox"
//    open docs/target/paradox/site/index.html
lazy val docs = (project in file("docs")).enablePlugins(ParadoxPlugin).
  settings(
    paradoxProperties += ("download_url" -> "https://github.com/guilhebl/offer-service")
  )