package com.raven.client.ai

import io.circe.Codec
import io.circe.generic.semiauto.deriveCodec

import java.time.Instant

final case class AiLog(
    modelId: String,
    confidenceScore: Double,
    responseTimeMs: Int,
    timestamp: Instant,
    numericFeatures: Map[String, Double],
    categoricalFeatures: Map[String, String]
) extends Serializable

object AiLog {

  val kafkaTopic: String = "ai-logs"

  implicit val aiLogCodec: Codec[AiLog] = deriveCodec
}
