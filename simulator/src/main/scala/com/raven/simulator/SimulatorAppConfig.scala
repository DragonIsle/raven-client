package com.raven.simulator

import cats.effect.IO
import org.http4s.Uri
import pureconfig.*
import pureconfig.error.UserValidationFailed
import pureconfig.module.catseffect.syntax.CatsEffectConfigSource

final case class SimulatorAppConfig(kafka: Map[String, String], apiBasePath: Uri) derives ConfigReader

object SimulatorAppConfig:

  given ConfigReader[Uri] =
    ConfigReader[String].emap(Uri.fromString(_).left.map(err => UserValidationFailed(err.getMessage)))

  val loadConfig: IO[SimulatorAppConfig] = ConfigSource.default.loadF[IO, SimulatorAppConfig]()
