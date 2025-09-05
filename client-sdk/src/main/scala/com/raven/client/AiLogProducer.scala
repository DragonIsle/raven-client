package com.raven.client

import cats.effect.IO
import com.raven.domain.AiLog

trait AiLogProducer {

  def produceAiLogs(aiLogs: Seq[AiLog]): IO[Unit]
  def produceStreamLogs(aiLogs: fs2.Stream[IO, AiLog]): IO[Unit]
}
