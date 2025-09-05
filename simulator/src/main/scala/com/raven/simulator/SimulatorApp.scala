package com.raven.simulator

import cats.effect.kernel.Clock
import cats.effect.std.Random
import cats.effect.{IO, IOApp}
import cats.syntax.parallel.*
import com.raven.client.{AiLogHttpProducer, AiLogKafkaProducer}
import com.raven.domain.AiLog
import com.raven.simulator.SimulatorAppConfig.SimulationConfig
import org.typelevel.log4cats.LoggerFactory
import org.typelevel.log4cats.slf4j.Slf4jFactory

import java.time.temporal.ChronoUnit
import scala.concurrent.duration.*

// Run the simulation and check metrics for results (Flink, Clickhouse, Kafka metrics)
object SimulatorApp extends IOApp.Simple:

  implicit val loggerFactory: LoggerFactory[IO] = Slf4jFactory.create[IO]

  val run: IO[Unit] = (for {
    config <- SimulatorAppConfig.loadConfig.toResource
    logProducer <-
      if (config.useKafka) AiLogKafkaProducer.load(config.kafka) else AiLogHttpProducer.load(config.ravenHostUri)
    _ <- (1 to config.simulation.parallelIngestors).toList
      .parTraverse(n => logProducer.produceStreamLogs(generateOneDeviceStream(n, config.simulation)))
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
    val intervalNanos = 1000000000 / logsAmount
    val values = Clock[IO].realTimeInstant.flatMap { ts =>
      (1 to logsAmount).toList.parTraverse(i =>
        AiLogGenerator.generateOneAiLog(ts.plus(i * intervalNanos, ChronoUnit.NANOS), config.singleLogGenerator)
      )
    }
    fs2.Stream
      .awakeEvery[IO](intervalNanos.nanos)
      .zip(fs2.Stream.evalSeq(values))
      .map { case (_, aiLog) => aiLog }
