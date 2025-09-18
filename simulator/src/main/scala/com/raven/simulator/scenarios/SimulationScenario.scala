package com.raven.simulator.scenarios

import cats.effect.IO
import com.raven.domain.AiLog
import com.raven.simulator.SimulatorAppConfig.SimulationConfig

trait SimulationScenario:
  def generateOneDeviceStream(config: SimulationConfig): fs2.Stream[IO, AiLog]
