package com.raven.simulator.context

import cats.effect.{IO, Resource}
import com.raven.client.ai.AiLog
import com.raven.simulator.SimulatorAppConfig
import fs2.kafka.{GenericSerializer, KafkaProducer, ProducerSettings, ValueSerializer}
import io.circe.syntax.*
import org.http4s.blaze.client.BlazeClientBuilder

final case class SimulatorContext(kafkaProducer: AiLogKafkaProducer)

object SimulatorContext:

  implicit val aiLogSerializer: Resource[IO, ValueSerializer[IO, AiLog]] =
    Resource.eval(IO.pure(GenericSerializer.string[IO].contramap[AiLog](_.asJson.noSpaces)))

  def load(config: SimulatorAppConfig): Resource[IO, SimulatorContext] = for {
    kafkaProducer <- KafkaProducer
      .resource(ProducerSettings[IO, String, AiLog].withProperties(config.kafka))
      .map(AiLogKafkaProducer(_))
    client <- BlazeClientBuilder[IO].resource
  } yield SimulatorContext(kafkaProducer)
