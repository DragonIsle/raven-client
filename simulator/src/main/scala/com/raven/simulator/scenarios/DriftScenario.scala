package com.raven.simulator.scenarios

import cats.effect.std.Random
import cats.effect.{Clock, IO}
import cats.syntax.parallel.*
import com.raven.domain.AiLog
import com.raven.simulator.AiLogGenerator.aiModelFeatureConfigs
import com.raven.simulator.SimulatorAppConfig.SimulationConfig
import com.raven.simulator.{AiLogGenerator, RandomUtils}

import java.time.temporal.ChronoUnit
import scala.concurrent.duration.*

final class DriftScenario(modelIds: Seq[String]) extends SimulationScenario:
  def generateOneDeviceStream(config: SimulationConfig): fs2.Stream[IO, AiLog] =
    for {
      given Random[IO] <- fs2.Stream.eval(Random.scalaUtilRandom[IO])
      currentSec <- fs2.Stream
        .awakeEvery[IO](1 second)
        .interruptAfter(config.totalDurationSeconds seconds)
        .map(_.toSeconds)
      logsAmount <- fs2.Stream.eval(
        config.spikesUpTo
          .map(upTo =>
            RandomUtils.rollChance(config.spikeChance).map(spike => if (spike) upTo else config.logsPerSecond)
          )
          .getOrElse(IO.pure(config.logsPerSecond))
      )
      shouldDrift = currentSec >= (config.totalDurationSeconds / 2)
      aiLog <- generateOneSecondStream(logsAmount, shouldDrift, config)
    } yield aiLog

  private def generateOneSecondStream(logsAmount: Int, shouldDrift: Boolean, config: SimulationConfig)(using
      random: Random[IO]
  ): fs2.Stream[IO, AiLog] =
    val intervalNanos = 1000000000 / logsAmount
    val values = Clock[IO].realTimeInstant.flatMap { ts =>
      (1 to logsAmount).toList.parTraverse(i =>
        random
          .elementOf(aiModelFeatureConfigs.keySet)
          .flatMap { modelId =>
            val updatedConfig =
              if (shouldDrift && modelIds.contains(modelId))
                config.singleLogGenerator.copy(driftNumChance = 1, driftCatChance = 1)
              else config.singleLogGenerator
            AiLogGenerator.generateOneAiLog(ts.plus(i * intervalNanos, ChronoUnit.NANOS), updatedConfig, modelId)
          }
      )
    }
    fs2.Stream
      .awakeEvery[IO](intervalNanos.nanos)
      .zip(fs2.Stream.evalSeq(values))
      .map { case (_, aiLog) => aiLog }
