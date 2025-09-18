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

  object Http4s {
    private val http4sVersion02316 = "0.23.16"
    private val http4sVersion02327 = "0.23.27"
    private val org                = "org.http4s"

    val client = org %% "http4s-blaze-client" % http4sVersion02316
    val circe  = org %% "http4s-circe"        % http4sVersion02327
    val dsl    = org %% "http4s-dsl"          % http4sVersion02327

    val deps: Seq[ModuleID] = Seq(Http4s.dsl, Http4s.circe, Http4s.client)
  }

  object ScalafixRules {
    val xuwei               = "com.github.xuwei-k" %% "scalafix-rules"     % "0.6.0"
    val typelevel           = "org.typelevel"      %% "typelevel-scalafix" % "0.4.0"
    val deps: Seq[ModuleID] = Seq(xuwei, typelevel)
  }

  object Cats {
    val deps: Seq[ModuleID] = Seq("org.typelevel" %% "cats-effect" % "3.6.3")
  }

  object Kafka {
    val fs2Kafka = "com.github.fd4s" %% "fs2-kafka" % "3.9.0"
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

  object GatlingLibs {
    private val gatlingVersion    = "3.14.4"
    private val gatling           = "io.gatling"            % "gatling-test-framework"    % gatlingVersion % Test
    private val gatlingHighcharts = "io.gatling.highcharts" % "gatling-charts-highcharts" % gatlingVersion % Test
    val deps: Seq[ModuleID]       = Seq(gatling, gatlingHighcharts)
  }
}
