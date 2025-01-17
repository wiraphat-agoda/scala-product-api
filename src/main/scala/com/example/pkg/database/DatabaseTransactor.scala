package com.example.pkg.database

import cats.effect.{Blocker, ContextShift, IO, Resource}
import doobie.hikari.HikariTransactor
import com.example.pkg.config.AppConfig
import doobie.util.ExecutionContexts

object DatabaseTransactor {
  def create(
              config: AppConfig
            )(implicit cs: ContextShift[IO]): Resource[IO, HikariTransactor[IO]] = {
    for {
      ce <- ExecutionContexts.fixedThreadPool[IO](32)
      be <- Blocker[IO]
      xa <- HikariTransactor.newHikariTransactor[IO](
        "org.postgresql.Driver",
        s"jdbc:postgresql://${config.database.host}:${config.database.port}/${config.database.name}",
        config.database.user,
        config.database.password,
        ce,
        be
      )
    } yield xa
  }
}