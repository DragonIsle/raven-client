package com.raven.simulator

import cats.effect.IO
import cats.effect.std.Random

object RandomUtils:
  def rollChance(chance: Double)(using Random[IO]): IO[Boolean] =
    Random[IO].betweenDouble(0.0, 1.0).map(_ < chance)
