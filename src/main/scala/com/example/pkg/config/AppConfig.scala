package com.example.pkg.config

import pureconfig._
import pureconfig.generic.auto._
import cats.effect.{Blocker, ContextShift, IO}
import pureconfig.generic.semiauto.deriveReader

case class DatabaseConfig(
                           host: String,
                           port: Int,
                           name: String,
                           user: String,
                           password: String
                         )

case class ServerConfig(
                         environment: String,
                         name: String,
                         host: String,
                         port: Int
                       )

case class KafkaConfig(
                        // Common configurations
                        bootstrapServers: String,
                        topic: String,
                        clientId: String,

                        // Producer specific configurations
                        acks: String = "all",
                        retries: Int = 3,
                        batchSize: Int = 16384,
                        lingerMs: Int = 1,
                        bufferMemory: Int = 33554432,

                        // Consumer specific configurations
                        groupId: String = "consumer-1",
                        autoOffsetReset: String = "earliest",
                        enableAutoCommit: Boolean = true,
                        autoCommitIntervalMs: Int = 1000,
                        sessionTimeoutMs: Int = 30000,
                        maxPollRecords: Int = 500,
                        fetchMinBytes: Int = 1,
                        fetchMaxWaitMs: Int = 500,
                        maxPartitionFetchBytes: Int = 1048576
                      )

case class RedisConfig(
                      host: String,
                      port: Int
                      )

object KafkaConfig {
  implicit val configReader: ConfigReader[KafkaConfig] = deriveReader[KafkaConfig]
}

case class AppConfig(
                      server: ServerConfig,
                      database: DatabaseConfig,
                      kafka: KafkaConfig,
                      redis: RedisConfig
                    )

object AppConfig {
  @volatile private var instance: Option[AppConfig] = None

  def load()(implicit cs: ContextShift[IO]): IO[AppConfig] = {
    Blocker[IO].use { _ =>
      IO.delay(ConfigSource.default.at("app").loadOrThrow[AppConfig])
    }
  }

  def getInstance(implicit cs: ContextShift[IO]): IO[AppConfig] = {
    instance match {
      case Some(config) => IO.pure(config)
      case None => this.synchronized {
        instance match {
          case Some(config) => IO.pure(config)
          case None =>
            load().map { config =>
              instance = Some(config)
              config
            }
        }
      }
    }
  }

  def clearInstance(): Unit = this.synchronized {
    instance = None
  }
}