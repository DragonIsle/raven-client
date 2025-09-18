package com.raven.simulator

import cats.effect.{IO, IOApp}
import cats.syntax.parallel.*
import com.raven.client.{AiLogHttpProducer, AiLogKafkaProducer}
import com.raven.simulator.scenarios.{CommonScenario, DriftScenario, LowAverageValuesScenario, SimulationScenario}
import org.typelevel.log4cats.LoggerFactory
import org.typelevel.log4cats.slf4j.Slf4jFactory

// Run the simulation and check metrics for results (Flink, Clickhouse, Kafka metrics)
object SimulatorApp extends IOApp.Simple:

  implicit val loggerFactory: LoggerFactory[IO] = Slf4jFactory.create[IO]

  val run: IO[Unit] = (for {
    config <- SimulatorAppConfig.loadConfig.toResource
    logProducer <-
      if (config.useKafka) AiLogKafkaProducer.load(config.kafka) else AiLogHttpProducer.load(config.ravenHostUri)
    // Scenario and running
//    scenario: SimulationScenario = CommonScenario
//    scenario: SimulationScenario = DriftScenario(AiLogGenerator.aiModelFeatureConfigs.keys.headOption.toSeq)
    scenario: SimulationScenario = LowAverageValuesScenario(AiLogGenerator.aiModelFeatureConfigs.keys.tail.toSeq)
    _ <- (1 to config.simulation.parallelIngestors).toList
      .parTraverse(_ => logProducer.produceStreamLogs(scenario.generateOneDeviceStream(config.simulation)))
      .toResource
  } yield ()).use(IO.pure)
