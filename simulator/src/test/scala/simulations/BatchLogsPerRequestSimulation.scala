package simulations

import com.raven.domain.AiLog
import io.circe.syntax.*
import io.gatling.core.Predef.*
import io.gatling.core.structure.ScenarioBuilder
import io.gatling.http.Predef.*
import io.gatling.http.protocol.HttpProtocolBuilder

import java.time.Instant
import scala.concurrent.duration.*
import scala.util.Random

class BatchLogsPerRequestSimulation extends Simulation:

  val httpProtocol: HttpProtocolBuilder =
    http
      .baseUrl("http://localhost:8080")
      .acceptHeader("application/json")
      .contentTypeHeader("application/json")

  val feeder: Iterator[Map[String, String]] = Iterator.continually {
    Map(
      "aiLog" -> (Seq.fill(10){AiLog(
          s"Model${Random.between(1, 4)}",
          Random.between(0.8, 1.0),
          Random.between(300, 400),
          Instant.now(),
          Map("numFeature1" -> 0.1, "numFeature2"       -> 0.3, "numFeature3" -> 0.2),
          Map("catFeature1" -> "string1", "catFeature2" -> "string2")
        )}).toList.asJson.noSpaces
    )
  }

  val scn: ScenarioBuilder = scenario("Log upload performance test (batch logs)")
    .feed(feeder)
    .exec(
      http("Post logs")
        .post("/input/ai/log/insert")
        .body(StringBody(session => s"${session("aiLog").as[String]}"))
        .asJson
        .check(status.is(200))
    )

  setUp(scn.inject(constantUsersPerSec(500) during 30.seconds)).protocols(httpProtocol)
