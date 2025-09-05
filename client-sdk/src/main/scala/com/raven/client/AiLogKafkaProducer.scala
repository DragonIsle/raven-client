package com.raven.client

import cats.effect.{IO, Resource}
import com.raven.client.AiLogKafkaProducer._
import com.raven.domain.AiLog
import fs2.kafka._
import io.circe.syntax._

final class AiLogKafkaProducer(aiLogProducer: KafkaProducer.PartitionsFor[IO, String, AiLog]) extends AiLogProducer {

  def produceAiLogs(aiLogs: Seq[AiLog]): IO[Unit] =
    fs2.Stream.evalSeq(IO.pure(aiLogs.map(mkRecordFromAiLog))).chunkAll.evalMap(aiLogProducer.produce).compile.drain

  def produceStreamLogs(aiLogs: fs2.Stream[IO, AiLog]): IO[Unit] =
    aiLogs.evalMap(log => aiLogProducer.produceOne_(mkRecordFromAiLog(log))).compile.drain
}

object AiLogKafkaProducer {

  private def mkRecordFromAiLog(aiLog: AiLog): ProducerRecord[String, AiLog] =
    ProducerRecord(AiLog.kafkaTopic, aiLog.modelId, aiLog)

  implicit val aiLogSerializer: Resource[IO, ValueSerializer[IO, AiLog]] =
    Resource.eval(IO.pure(GenericSerializer.string[IO].contramap[AiLog](_.asJson.noSpaces)))

  def load(kafkaProperties: Map[String, String]): Resource[IO, AiLogKafkaProducer] =
    KafkaProducer
      .resource(ProducerSettings[IO, String, AiLog].withProperties(kafkaProperties))
      .map(new AiLogKafkaProducer(_))
}
