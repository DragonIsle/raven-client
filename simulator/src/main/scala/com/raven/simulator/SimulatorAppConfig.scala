package com.raven.simulator

import cats.effect.IO
import com.raven.simulator.SimulatorAppConfig.SimulationConfig
import org.http4s.Uri
import pureconfig.*
import pureconfig.error.UserValidationFailed
import pureconfig.module.catseffect.syntax.CatsEffectConfigSource

// todo: remove apiBasePath?
final case class SimulatorAppConfig(kafka: Map[String, String], apiBasePath: Uri, simulation: SimulationConfig)
    derives ConfigReader

object SimulatorAppConfig:

  final case class SingleLogGeneratorConfig(
      confidenceScoreThreshold: Double,
      responseTimeMsThreshold: Int,
      driftNumChance: Double,
      driftCatChance: Double,
      lowConfidenceScoreChance: Double,
      highResponseTimeChance: Double
  )

  final case class SimulationConfig(
      parallelIngestors: Int,
      totalDurationSeconds: Int,
      logsPerSecond: Int,
      spikesUpTo: Option[Int],
      spikeChance: Double,
      singleLogGenerator: SingleLogGeneratorConfig
  ) derives ConfigReader

  given ConfigReader[Uri] =
    ConfigReader[String].emap(Uri.fromString(_).left.map(err => UserValidationFailed(err.getMessage)))

  val loadConfig: IO[SimulatorAppConfig] = ConfigSource.default.loadF[IO, SimulatorAppConfig]()
