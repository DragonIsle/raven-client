package com.raven.simulator.context

import cats.effect.IO
import com.raven.client.ai.AiLog
import com.raven.simulator.context.AiLogKafkaProducer.*
import fs2.kafka.*

final class AiLogKafkaProducer(aiLogProducer: KafkaProducer.PartitionsFor[IO, String, AiLog]):

  def produceAiLogs(aiLogs: Seq[AiLog]): IO[Unit] =
    fs2.Stream.evalSeq(IO.pure(aiLogs.map(mkRecordFromAiLog))).chunkAll.evalMap(aiLogProducer.produce).compile.drain

  def produceStreamLogs(aiLogs: fs2.Stream[IO, AiLog]): IO[Unit] =
    aiLogs.evalMap(mkRecordFromAiLog andThen aiLogProducer.produceOne_).compile.drain

object AiLogKafkaProducer:

  private def mkRecordFromAiLog(aiLog: AiLog): ProducerRecord[String, AiLog] =
    ProducerRecord(AiLog.kafkaTopic, aiLog.modelId, aiLog)
