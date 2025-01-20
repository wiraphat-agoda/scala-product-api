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
                        bootstrapServers: String,
                        topic: String,
                        clientId: String,
                        acks: String = "all",
                        retries: Int = 3,
                        batchSize: Int = 16384,
                        lingerMs: Int = 1,
                        bufferMemory: Int = 33554432
                      )

object KafkaConfig {
  implicit val configReader: ConfigReader[KafkaConfig] = deriveReader[KafkaConfig]
}

case class AppConfig(
                      server: ServerConfig,
                      database: DatabaseConfig,
                      kafka: KafkaConfig
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