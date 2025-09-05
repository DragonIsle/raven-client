package com.raven.client

import cats.effect.{Async, IO, Resource}
import com.raven.client.AiLogHttpProducer.mkRequest
import com.raven.domain.AiLog
import org.http4s.{Headers, MediaType, Method, Request, Uri}
import org.http4s.client.Client
import io.circe.syntax.EncoderOps
import org.http4s.blaze.client.BlazeClientBuilder
import org.http4s.headers.`Content-Type`
import org.typelevel.log4cats.{Logger, LoggerFactory}

class AiLogHttpProducer(host: Uri, client: Client[IO])(implicit loggerFactory: LoggerFactory[IO])
    extends AiLogProducer {

  private val logger: Logger[IO] = loggerFactory.getLogger

  def produceAiLogs(aiLogs: Seq[AiLog]): IO[Unit] =
    client
      .run(mkRequest(host, aiLogs))
      .use { response =>
        if (response.status.isSuccess)
          IO.unit
        else
          logger.warn(s"Error during produce logs request, status: ${response.status}, body: ${response.body}")
      }

  def produceStreamLogs(aiLogs: fs2.Stream[IO, AiLog]): IO[Unit] =
    aiLogs.evalMap(log => produceAiLogs(Seq(log))).compile.drain

}
object AiLogHttpProducer {
  private def mkRequest(host: Uri, aiLogs: Seq[AiLog]): Request[IO] = {
    Request[IO](method = Method.POST, uri = host / "input" / "ai" / "log" / "insert")
      .withEntity(aiLogs.asJson.noSpaces)
      .withHeaders(Headers(`Content-Type`(MediaType.application.json)))
  }

  def load(ravenHostUri: Uri)(implicit loggerFactory: LoggerFactory[IO]): Resource[IO, AiLogHttpProducer] =
    BlazeClientBuilder[IO].resource.map(client => new AiLogHttpProducer(ravenHostUri, client))
}
