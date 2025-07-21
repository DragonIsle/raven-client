package com.raven.simulator

import cats.effect.kernel.Clock
import cats.effect.std.Random
import cats.effect.{IO, IOApp}
import cats.syntax.parallel.*
import com.raven.client.ai.AiLog
import com.raven.simulator.SimulatorAppConfig.SimulationConfig
import com.raven.simulator.context.*

import scala.concurrent.duration.*

// Run the simulation and check metrics for results (Flink, Clickhouse, Kafka metrics)
object SimulatorApp extends IOApp.Simple:

  val run: IO[Unit] = (for {
    config  <- SimulatorAppConfig.loadConfig.toResource
    context <- SimulatorContext.load(config)
    _ <- (1 to config.simulation.parallelIngestors).toList
      .parTraverse(n => context.kafkaProducer.produceStreamLogs(generateOneDeviceStream(n, config.simulation)))
      .toResource
  } yield ()).use(IO.pure)

  private def generateOneDeviceStream(deviceNumber: Int, config: SimulationConfig): fs2.Stream[IO, AiLog] =
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
      Random[IO]
  ): fs2.Stream[IO, AiLog] =
    fs2.Stream
      .awakeEvery[IO]((1000000 / logsAmount).micros)
      .evalMap(_ => Clock[IO].realTimeInstant.flatMap(AiLogGenerator.generateOneAiLog(_, config.singleLogGenerator)))
      .interruptAfter(1 second)
