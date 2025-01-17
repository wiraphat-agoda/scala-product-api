package com.example.pkg.config

import pureconfig._
import pureconfig.generic.auto._
import cats.effect.{Blocker, ContextShift, IO}

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

case class AppConfig(
                      server: ServerConfig,
                      database: DatabaseConfig
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