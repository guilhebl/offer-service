// The Play plugin
addSbtPlugin("com.typesafe.play" % "sbt-plugin" % "2.7.0")

// sbt-paradox, used for documentation
addSbtPlugin("com.lightbend.paradox" % "sbt-paradox" % "0.4.4")

// Load testing tool:
// http://gatling.io/docs/2.2.2/extensions/sbt_plugin.html
addSbtPlugin("io.gatling" % "gatling-sbt" % "2.2.2")

// Scala formatting: "sbt scalafmt"
addSbtPlugin("com.lucidchart" % "sbt-scalafmt" % "1.15")

// SBT Native
addSbtPlugin("com.typesafe.sbt" % "sbt-native-packager" % "1.3.15")

// Scoverage - Scala Test coverage
addSbtPlugin("org.scoverage" % "sbt-scoverage" % "1.5.1")
