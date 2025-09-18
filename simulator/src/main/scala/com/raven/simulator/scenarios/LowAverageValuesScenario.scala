package com.raven.simulator.scenarios

import cats.effect.std.Random
import cats.effect.{Clock, IO}
import cats.implicits.catsSyntaxEq
import cats.syntax.parallel.*
import com.raven.domain.AiLog
import com.raven.simulator.AiLogGenerator.aiModelFeatureConfigs
import com.raven.simulator.SimulatorAppConfig.SimulationConfig
import com.raven.simulator.{AiLogGenerator, RandomUtils}

import java.time.temporal.ChronoUnit
import scala.concurrent.duration.*

final class LowAverageValuesScenario(modelIds: Seq[String]) extends SimulationScenario:
  def generateOneDeviceStream(config: SimulationConfig): fs2.Stream[IO, AiLog] =
    for {
      given Random[IO] <- fs2.Stream.eval(Random.scalaUtilRandom[IO])
      currentMin <- fs2.Stream
        .awakeEvery[IO](1 second)
        .interruptAfter(config.totalDurationSeconds seconds)
        .map(_.toMinutes)
      logsAmount <- fs2.Stream.eval(
        config.spikesUpTo
          .map(upTo =>
            RandomUtils.rollChance(config.spikeChance).map(spike => if (spike) upTo else config.logsPerSecond)
          )
          .getOrElse(IO.pure(config.logsPerSecond))
      )
      shouldBeLow = currentMin % 3 === 0
      aiLog <- generateOneSecondStream(logsAmount, shouldBeLow, config)
    } yield aiLog

  private def generateOneSecondStream(logsAmount: Int, shouldBeLow: Boolean, config: SimulationConfig)(using
      random: Random[IO]
  ): fs2.Stream[IO, AiLog] =
    val intervalNanos = 1000000000 / logsAmount
    val values = Clock[IO].realTimeInstant.flatMap { ts =>
      (1 to logsAmount).toList.parTraverse(i =>
        random
          .elementOf(aiModelFeatureConfigs.keySet)
          .flatMap { modelId =>
            val updatedConfig =
              if (shouldBeLow && modelIds.contains(modelId))
                config.singleLogGenerator.copy(
                  confidenceScoreThreshold = Math.max(config.singleLogGenerator.confidenceScoreThreshold - 0.2, 0),
                  highResponseTimeChance   = config.singleLogGenerator.responseTimeMsThreshold + 200
                )
              else config.singleLogGenerator
            AiLogGenerator.generateOneAiLog(ts.plus(i * intervalNanos, ChronoUnit.NANOS), updatedConfig, modelId)
          }
      )
    }
    fs2.Stream
      .awakeEvery[IO](intervalNanos.nanos)
      .zip(fs2.Stream.evalSeq(values))
      .map { case (_, aiLog) => aiLog }
