# Raven Client

A Scala-based AI logging system with Kafka integration and simulation capabilities.

## Overview

Raven Client is a multi-module Scala project that provides:
- **Client SDK**: A library for producing AI logs to Kafka streams
- **Domain Models**: Core data structures for AI logging
- **Simulator**: An application for simulating AI log production

## Modules

### 📦 Client SDK (`client-sdk`)

A high-performance Scala library for producing AI logs to Kafka streams.

**Features:**
- Built on fs2-kafka for efficient streaming
- Support for both batch and stream processing
- Cross-compiled for Scala 2.12.18 and 3.7.0
- Type-safe AI log production

**Installation:**
```scala
resolvers += "GitHub Package Registry" at "https://maven.pkg.github.com/dragonisle/raven-client"
libraryDependencies += "com.raven" %% "client-sdk" % "0.2.1-SNAPSHOT"
```

[📖 Full Client SDK Documentation](./client-sdk/README.md)

### 🏗️ Domain (`domain`)

Core data models and structures for AI logging.

**Installation:**
```scala
libraryDependencies += "com.raven" %% "domain" % "0.2.1-SNAPSHOT"
```

### 🎯 Simulator (`simulator`)

A Docker-containerized application for simulating AI log production and testing the client SDK.

## Quick Start

### Using the Client SDK

```scala
import cats.effect.{IO, IOApp}
import com.raven.client.AiLogKafkaProducer
import com.raven.domain.AiLog
import java.time.Instant

object Example extends IOApp.Simple {
  val kafkaConfig = Map(
    "bootstrap.servers" -> "localhost:9092"
  )
  
  val sampleLog = AiLog(
    modelId = "gpt-4",
    confidenceScore = 0.95,
    responseTimeMs = 150,
    timestamp = Instant.now(),
    numericFeatures = Map("temperature" -> 0.7),
    categoricalFeatures = Map("task" -> "completion")
  )
  
  def run: IO[Unit] = 
    AiLogKafkaProducer.load(kafkaConfig).use { producer =>
      producer.produceAiLogs(Seq(sampleLog))
    }
}
```

## Development

### Prerequisites

- JDK 21
- SBT 1.x
- Docker (for simulator)

### Building

```bash
# Compile all modules
sbt compile

# Run tests
sbt test

# Build Docker image for simulator
sbt simulator/docker:publishLocal
```

### Publishing

The project is configured to publish to GitHub Packages. Publishing happens automatically via GitHub Actions on pushes to main/master branches.

**Manual Publishing:**
```bash
# Set GitHub token
export GITHUB_TOKEN=your_github_token

# Publish domain module
sbt domain/publish

# Publish client-sdk module
sbt clientSdk/publish
```

### Authentication for GitHub Packages

To use the published packages, you need to authenticate with GitHub Packages:

1. Create a GitHub Personal Access Token with `read:packages` permission
2. Add to `~/.sbt/1.0/credentials`:
```
realm=GitHub Package Registry
host=maven.pkg.github.com
user=YOUR_GITHUB_USERNAME
password=YOUR_GITHUB_TOKEN
```

Or set the `GITHUB_TOKEN` environment variable.

## Architecture

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   Simulator     │    │   Client SDK    │    │     Domain      │
│                 │───▶│                 │───▶│                 │
│ - Generates     │    │ - Kafka Prod.   │    │ - AiLog Model   │
│   AI Logs       │    │ - Streaming     │    │ - Serialization │
│ - Docker App    │    │ - Batching      │    │ - Core Types    │
└─────────────────┘    └─────────────────┘    └─────────────────┘
                                │
                                ▼
                       ┌─────────────────┐
                       │     Kafka       │
                       │   (ai-logs)     │
                       └─────────────────┘
```

## Configuration

### Kafka Setup

The system expects a Kafka cluster with the `ai-logs` topic. For local development:

```bash
# Start Kafka with Docker Compose
docker-compose up -d kafka

# Create topic
kafka-topics --create --topic ai-logs --bootstrap-server localhost:9092
```

### Simulator Configuration

The simulator can be configured via `application.conf` or environment variables. See `simulator.yaml` for Kubernetes deployment.

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
- Check module-specific documentation
- Review the examples in this README