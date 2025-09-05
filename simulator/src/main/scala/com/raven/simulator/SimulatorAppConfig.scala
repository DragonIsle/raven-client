package com.raven.simulator

import cats.effect.IO
import com.raven.simulator.SimulatorAppConfig.SimulationConfig
import org.http4s.Uri
import pureconfig.*
import pureconfig.error.UserValidationFailed
import pureconfig.module.catseffect.syntax.CatsEffectConfigSource

final case class SimulatorAppConfig(
    kafka: Map[String, String],
    useKafka: Boolean,
    ravenHostUri: Uri,
    simulation: SimulationConfig
) derives ConfigReader

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

  private implicit val uriReader: ConfigReader[Uri] =
    ConfigReader[String].emap(Uri.fromString(_).left.map(err => UserValidationFailed(err.getMessage)))

  val loadConfig: IO[SimulatorAppConfig] = ConfigSource.default.loadF[IO, SimulatorAppConfig]()
