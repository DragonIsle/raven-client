package com.raven.simulator

import cats.effect.IO
import cats.effect.std.Random
import cats.syntax.traverse.*
import com.raven.domain.AiLog
import com.raven.simulator.SimulatorAppConfig.SingleLogGeneratorConfig

import java.time.Instant
import scala.math.BigDecimal.RoundingMode

object AiLogGenerator:

  final case class NumericFeatureConfig(min: Double, max: Double, precision: Int)

  val aiModelFeatureConfigs: Map[String, (Map[String, NumericFeatureConfig], Map[String, List[String]])] = Map(
    "model1" -> (Map(
      "tenure_months"     -> NumericFeatureConfig(1.0, 36.0, 0),
      "monthly_charges"   -> NumericFeatureConfig(1.0, 100.0, 2),
      "total_charges"     -> NumericFeatureConfig(1.0, 10000.0, 2),
      "num_support_calls" -> NumericFeatureConfig(1.0, 10.0, 0)
    ), Map(
      "contract_type"    -> List("monthly", "early"),
      "payment_method"   -> List("credit_card", "bank_transfer"),
      "customer_segment" -> List("business", "personal")
    )),
    "model2" -> (Map(
      "transaction_amount"        -> NumericFeatureConfig(1.0, 100000.0, 2),
      "transaction_hour"          -> NumericFeatureConfig(0.0, 23.0, 0),
      "card_age_days"             -> NumericFeatureConfig(1.0, 7000.0, 0),
      "num_transactions_today"    -> NumericFeatureConfig(1.0, 100.0, 0),
      "avg_transaction_amount_7d" -> NumericFeatureConfig(1.0, 100000.0, 2)
    ), Map(
      "card_type"         -> List("debit", "credit", "prepaid"),
      "merchant_category" -> List("electronics", "travel", "grocery", "closes")
    )),
    "model3" -> (Map(
      "ad_position"               -> NumericFeatureConfig(1.0, 5.0, 0),
      "num_previous_clicks"       -> NumericFeatureConfig(1.0, 20.0, 0),
      "user_session_length_sec"   -> NumericFeatureConfig(1.0, 36000.0, 0),
      "time_since_last_click_sec" -> NumericFeatureConfig(1.0, 30000.0, 0)
    ), Map(
      "ad_category" -> List("tech", "fashion", "sport", "entertainment", "health"),
      "device_type" -> List("mobile", "desktop", "tablet"),
      "user_region" -> List("Africa", "Asia", "Europe", "North America", "South America", "Australia")
    ))
  )

  def generateOneAiLog(
      timestamp: Instant,
      config: SingleLogGeneratorConfig,
      modelId: String
  )(using random: Random[IO]): IO[AiLog] =
    for {
      lowConfidenceScore <- RandomUtils.rollChance(config.lowConfidenceScoreChance)
      highResponseTime   <- RandomUtils.rollChance(config.highResponseTimeChance)
      driftNum           <- RandomUtils.rollChance(config.driftNumChance)
      driftCat           <- RandomUtils.rollChance(config.driftCatChance)
      confidenceScore <-
        if (lowConfidenceScore) IO.pure(config.confidenceScoreThreshold - 0.001)
        else random.betweenDouble(config.confidenceScoreThreshold, 1.0)
      responseTimeMs <-
        if (highResponseTime) IO.pure(config.responseTimeMsThreshold + 1)
        else random.betweenInt(100, config.responseTimeMsThreshold)
      (numF, catF) = aiModelFeatureConfigs.getOrElse(modelId, (Map.empty, Map.empty))
      numericFeatures <- numF.toList.traverse { case (feature, NumericFeatureConfig(from, to, precision)) =>
        // numeric drift - 20% distribution shift
        val driftAddition = if (driftNum) (to - from) / 5 else 0.0
        random
          .betweenDouble(from, to)
          .map(v => feature -> BigDecimal.valueOf(v + driftAddition).setScale(precision, RoundingMode.HALF_UP).toDouble)
      }
      catFeatures <- catF.toList.traverse { case (feature, possibleValues) =>
        // categorical drift - first value is always more probable then others
        val resListToChoose = if (driftCat) List.fill(4)(possibleValues.head) ++ possibleValues.tail else possibleValues
        random.elementOf(resListToChoose).map(feature -> _)
      }
    } yield AiLog(modelId, confidenceScore, responseTimeMs, timestamp, numericFeatures.toMap, catFeatures.toMap)
