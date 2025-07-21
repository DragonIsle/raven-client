import sbt.*

object Dependencies {

  object Circe {
    val circeVersion = "0.14.10"

    val deps: Seq[ModuleID] = Seq(
      "io.circe" %% "circe-core",
      "io.circe" %% "circe-generic",
      "io.circe" %% "circe-parser"
    ).map(_ % circeVersion)
  }

  object ScalafixRules {
    val xuwei               = "com.github.xuwei-k" %% "scalafix-rules"     % "0.6.0"
    val typelevel           = "org.typelevel"      %% "typelevel-scalafix" % "0.4.0"
    val deps: Seq[ModuleID] = Seq(xuwei, typelevel)
  }

  object Cats {
    val deps: Seq[ModuleID] = Seq("org.typelevel" %% "cats-effect" % "3.6.2")
  }

  object Kafka {
    val fs2Kafka = "com.github.fd4s" %% "fs2-kafka" % "3.8.0"
  }

  object Config {
    val pureConfigCats      = "com.github.pureconfig" %% "pureconfig-cats-effect" % "0.17.9"
    val deps: Seq[ModuleID] = Seq(pureConfigCats)
  }

  object Log {
    private val log4catsVersion = "2.7.1"

    val log4cats            = "org.typelevel" %% "log4cats-slf4j"  % log4catsVersion
    val logbackClassic      = "ch.qos.logback" % "logback-classic" % "1.5.18"
    val deps: Seq[ModuleID] = Seq(log4cats, logbackClassic)
  }
}
