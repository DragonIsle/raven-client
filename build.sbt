import Dependencies.*

import scala.collection.Seq

ThisBuild / organization := "com.raven"
ThisBuild / version := "0.2.0-SNAPSHOT"
ThisBuild / scalaVersion := "3.7.0"
ThisBuild / semanticdbEnabled := true

val wartExclusions = Seq(Wart.Any, Wart.Nothing, Wart.ImplicitParameter)

val sourceCodeSettings = Seq(
  wartremoverErrors ++= Warts.unsafe,
  wartremoverExcluded += sourceManaged.value,
  scalacOptions := Seq(
    "-unchecked",
    "-deprecation",
    "-feature",
    "-language:higherKinds",
    "-language:existentials",
    "-language:implicitConversions",
    "-language:postfixOps",
    "-encoding",
    "utf8",
    "-Ywarn-unused-import", // Changed from -Wunused:imports
    "-Wconf:src=src_managed/.*:silent"
  )
)

lazy val root = (project in file("."))
  .aggregate(simulator, clientSdk)
  .settings(
    name := "raven-client",
    publish / skip := true
  )

lazy val simulator = (project in file("simulator"))
  .settings(sourceCodeSettings)
  .enablePlugins(JavaAppPackaging, DockerPlugin, ScalafixPlugin)
  .settings(
    name := "simulator",
    Compile / run / fork := true,
    libraryDependencies ++= Http4s.deps ++ Config.deps ++ Cats.deps ++ Log.deps :+ Kafka.fs2Kafka,
    scalafixDependencies ++= ScalafixRules.deps,
    scalafixOnCompile := true,
    dockerExposedPorts ++= Seq(8080),
    dockerBaseImage := "eclipse-temurin:21-jre-jammy",
    dockerRepository := Some("ghcr.io/dragonisle"),
    dockerEnvVars := Map(
      "JAVA_OPTS" -> "-Dconfig.file=/etc/config/application.conf"
    ),
    Docker / maintainer := "Raven Team",
    Docker / packageName := "simulator"
  )
  .dependsOn(clientSdk)

lazy val clientSdk = (project in file("client-sdk"))
  .settings(
    name := "client-sdk",
    crossScalaVersions := Seq("2.12.18", "3.7.0"),
    libraryDependencies ++= Circe.deps
  )
