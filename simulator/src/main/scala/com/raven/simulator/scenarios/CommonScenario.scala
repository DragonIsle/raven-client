package com.raven.simulator.scenarios

import cats.effect.{Clock, IO}
import cats.syntax.parallel.*
import cats.effect.std.Random
import com.raven.domain.AiLog
import com.raven.simulator.AiLogGenerator.aiModelFeatureConfigs
import com.raven.simulator.{AiLogGenerator, RandomUtils}
import com.raven.simulator.SimulatorAppConfig.SimulationConfig

import java.time.temporal.ChronoUnit
import scala.concurrent.duration.*

object CommonScenario extends SimulationScenario:
  def generateOneDeviceStream(config: SimulationConfig): fs2.Stream[IO, AiLog] =
    for {
      given Random[IO] <- fs2.Stream.eval(Random.scalaUtilRandom[IO])
      _                <- fs2.Stream.awakeEvery[IO](1 second).interruptAfter(config.totalDurationSeconds seconds)
      logsAmount <- fs2.Stream.eval(
        config.spikesUpTo
          .map(upTo =>
            RandomUtils.rollChance(config.spikeChance).map(spike => if (spike) upTo else config.logsPerSecond)
          )
          .getOrElse(IO.pure(config.logsPerSecond))
      )
      aiLog <- generateOneSecondStream(logsAmount, config)
    } yield aiLog

  private def generateOneSecondStream(logsAmount: Int, config: SimulationConfig)(using
      random: Random[IO]
  ): fs2.Stream[IO, AiLog] =
    val intervalNanos = 1000000000 / logsAmount
    val values = Clock[IO].realTimeInstant.flatMap { ts =>
      (1 to logsAmount).toList.parTraverse(i =>
        random
          .elementOf(aiModelFeatureConfigs.keySet)
          .flatMap(
            AiLogGenerator.generateOneAiLog(ts.plus(i * intervalNanos, ChronoUnit.NANOS), config.singleLogGenerator, _)
          )
      )
    }
    fs2.Stream
      .awakeEvery[IO](intervalNanos.nanos)
      .zip(fs2.Stream.evalSeq(values))
      .map { case (_, aiLog) => aiLog }
