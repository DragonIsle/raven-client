# Raven Client SDK

A Scala library for producing AI logs to Kafka streams with support for both Scala 2.12 and 3.x.

## Overview

The Raven Client SDK provides a simple and efficient way to produce AI model logs to Kafka topics. It's built on top of the fs2-kafka library and provides both batch and streaming capabilities for AI log production.

## Features

- 🚀 **High Performance**: Built on fs2-kafka for efficient streaming
- 📊 **AI Log Production**: Specialized for AI model logging with structured data
- 🔄 **Streaming Support**: Both batch and stream-based log production
- 🎯 **Type Safe**: Leverages Scala's type system for compile-time safety
- 🔧 **Configurable**: Flexible Kafka configuration options
- 📦 **Cross-Platform**: Supports Scala 2.12.18 and 3.7.0

## Installation

### SBT

Add the following to your `build.sbt`:

```scala
resolvers += "GitHub Package Registry" at "https://maven.pkg.github.com/dragonisle/raven-client"

libraryDependencies += "com.raven" %% "client-sdk" % "0.2.1-SNAPSHOT"
```

### Authentication

To access packages from GitHub Package Registry, you need to authenticate:

1. Create a GitHub Personal Access Token with `read:packages` permission
2. Add credentials to your `~/.sbt/1.0/credentials` file:

```
realm=GitHub Package Registry
host=maven.pkg.github.com
user=YOUR_GITHUB_USERNAME
password=YOUR_GITHUB_TOKEN
```

Or set the `GITHUB_TOKEN` environment variable.

## Quick Start

### Basic Usage

```scala
import cats.effect.{IO, IOApp}
import com.raven.client.AiLogHttpProducer
import com.raven.domain.AiLog
import java.time.Instant
import org.http4s.Uri
import org.typelevel.log4cats.LoggerFactory
import org.typelevel.log4cats.slf4j.Slf4jFactory

object Example extends IOApp.Simple {

  private implicit val loggerFactory: LoggerFactory[IO] = Slf4jFactory.create[IO]

  val sampleLog = AiLog(
    modelId = "model1",
    confidenceScore = 0.95,
    responseTimeMs = 150,
    timestamp = Instant.now(),
    numericFeatures = Map("temperature" -> 0.7, "tokens" -> 100.0),
    categoricalFeatures = Map("task" -> "completion", "version" -> "v1")
  )

  def run: IO[Unit] =
    AiLogHttpProducer.load(Uri("http://localhost:8080")).use { producer =>
      producer.produceAiLogs(Seq(sampleLog))
    }
}
```

### Streaming Usage

```scala
import fs2.Stream
import cats.effect.IO

// Create a stream of AI logs
val logStream: Stream[IO, AiLog] = Stream.eval(IO.pure(sampleLog)).repeat.take(1000)

// Produce the stream
AiLogKafkaProducer.load(kafkaConfig).use { producer =>
  producer.produceStreamLogs(logStream)
}
```

## API Reference

### AiLogProducer

The main trait for producing AI logs:

```scala
trait AiLogProducer {
  def produceAiLogs(aiLogs: Seq[AiLog]): IO[Unit]
  def produceStreamLogs(aiLogs: fs2.Stream[IO, AiLog]): IO[Unit]
}
```

#### Methods

- **`produceAiLogs(aiLogs: Seq[AiLog]): IO[Unit]`**
  - Produces a batch of AI logs to Kafka
  - Parameters: `aiLogs` - Sequence of AiLog instances to produce
  - Returns: IO effect that completes when all logs are produced

- **`produceStreamLogs(aiLogs: fs2.Stream[IO, AiLog]): IO[Unit]`**
  - Produces a stream of AI logs to Kafka
  - Parameters: `aiLogs` - fs2 Stream of AiLog instances
  - Returns: IO effect that completes when the stream is fully consumed

### AiLogKafkaProducer

Kafka-specific implementation of AiLogProducer:

```scala
class AiLogKafkaProducer(aiLogProducer: KafkaProducer.PartitionsFor[IO, String, AiLog]) 
  extends AiLogProducer
```

#### Factory Methods

- **`AiLogKafkaProducer.load(kafkaProperties: Map[String, String]): Resource[IO, AiLogKafkaProducer]`**
  - Creates a new AiLogKafkaProducer with the given Kafka configuration
  - Parameters: `kafkaProperties` - Map of Kafka producer configuration properties
  - Returns: Resource that manages the producer lifecycle

## Configuration

### Kafka Properties

Common Kafka producer properties you might want to configure:

```scala
val kafkaConfig = Map(
  "bootstrap.servers" -> "localhost:9092",
  "acks" -> "all",
  "retries" -> "3",
  "batch.size" -> "16384",
  "linger.ms" -> "1",
  "buffer.memory" -> "33554432",
  "key.serializer" -> "org.apache.kafka.common.serialization.StringSerializer"
)
```

### Topic Configuration

AI logs are produced to the topic defined in `AiLog.kafkaTopic` (default: "ai-logs"). The message key is the `modelId` from the AiLog.

## Data Model

### AiLog

The core data structure for AI model logs:

```scala
case class AiLog(
  modelId: String,                              // Identifier for the AI model
  confidenceScore: Double,                      // Confidence score (0.0 to 1.0)
  responseTimeMs: Int,                         // Response time in milliseconds
  timestamp: Instant,                          // When the log was created
  numericFeatures: Map[String, Double],        // Numeric features/metrics
  categoricalFeatures: Map[String, String]     // Categorical features/metadata
)
```

## Error Handling

The library uses cats-effect IO for error handling. Common error scenarios:

```scala
import cats.effect.IO

AiLogKafkaProducer.load(kafkaConfig).use { producer =>
  producer.produceAiLogs(logs)
}.handleErrorWith { error =>
  IO.println(s"Failed to produce logs: ${error.getMessage}")
}
```

## Performance Considerations

- **Batching**: Use `produceAiLogs` for batch processing when you have multiple logs
- **Streaming**: Use `produceStreamLogs` for continuous data streams
- **Resource Management**: Always use the Resource-based factory method to ensure proper cleanup
- **Backpressure**: The fs2-kafka integration handles backpressure automatically

## Dependencies

This library depends on:

- **cats-effect**: For functional effects and resource management
- **fs2-kafka**: For Kafka integration
- **circe**: For JSON serialization (via domain module)
- **raven-domain**: For the AiLog data model

## Examples

### Batch Processing

```scala
val logs = (1 to 100).map { i =>
  AiLog(
    modelId = s"model-$i",
    confidenceScore = 0.8 + (i % 20) * 0.01,
    responseTimeMs = 100 + i,
    timestamp = Instant.now(),
    numericFeatures = Map("iteration" -> i.toDouble),
    categoricalFeatures = Map("batch" -> "example")
  )
}

AiLogKafkaProducer.load(kafkaConfig).use(_.produceAiLogs(logs))
```

### Stream Processing with Error Handling

```scala
val logStream = Stream.repeatEval {
  IO.delay {
    AiLog(
      modelId = "streaming-model",
      confidenceScore = scala.util.Random.nextDouble(),
      responseTimeMs = scala.util.Random.nextInt(500),
      timestamp = Instant.now(),
      numericFeatures = Map("random" -> scala.util.Random.nextDouble()),
      categoricalFeatures = Map("source" -> "stream")
    )
  }
}.take(1000)

AiLogKafkaProducer.load(kafkaConfig).use { producer =>
  producer.produceStreamLogs(logStream)
}.handleErrorWith { error =>
  IO.println(s"Stream processing failed: $error") *> IO.raiseError(error)
}
```

## Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Add tests
5. Submit a pull request

## License

[Add your license information here]

## Support

For issues and questions:
- Create an issue on GitHub
- Check the documentation
- Review the examples in this README
