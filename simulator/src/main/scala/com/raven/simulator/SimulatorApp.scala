package com.raven.simulator

import cats.effect.kernel.Clock
import cats.effect.std.Random
import cats.effect.{IO, IOApp}
import cats.syntax.parallel.*
import com.raven.client.ai.AiLog
import com.raven.simulator.context.*

import scala.concurrent.duration.*

// Run the simulation and check metrics for results (Flink, Clickhouse, Kafka metrics)
object SimulatorApp extends IOApp.Simple:

  private val deviceAmount         = 5
  private val totalDurationSeconds = 60
  private val logsPerSecond        = 1000
  private val spikesUpTo           = Some(5000)
  private val spikeChance          = 0.05

  val run: IO[Unit] = (for {
    context <- SimulatorContext.load
    _ <- (1 to deviceAmount).toList
      .parTraverse(n => context.kafkaProducer.produceStreamLogs(generateOneDeviceStream(n)))
      .toResource
  } yield ()).use(IO.pure)

  private def generateOneDeviceStream(deviceNumber: Int): fs2.Stream[IO, AiLog] = for {
    random <- fs2.Stream.eval(Random.scalaUtilRandom[IO])
    _      <- fs2.Stream.awakeEvery[IO](1 second).interruptAfter(totalDurationSeconds seconds)
    logsAmount <- fs2.Stream.eval(
      spikesUpTo
        .map(upTo => random.betweenDouble(0.0, 1.0).map(r => if (r < spikeChance) upTo else logsPerSecond))
        .getOrElse(IO.pure(logsPerSecond))
    )
    aiLog <- generateOneSecondStream(logsAmount)
  } yield aiLog

  private def generateOneSecondStream(logsAmount: Int)(using  Random[IO]): fs2.Stream[IO, AiLog] =
    fs2.Stream
      .awakeEvery[IO]((1000000 / logsAmount).micros)
      .evalMap(_ => Clock[IO].realTimeInstant.flatMap(AiLogGenerator.generateOneAiLog(_)))
      .interruptAfter(1 second)
