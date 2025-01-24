package com.example.internal.adapter.infrastructure.database

import cats.effect.{Async, Blocker, ContextShift, Resource}
import com.example.pkg.config.DatabaseConfig
import doobie.hikari.HikariTransactor
import doobie.util.ExecutionContexts

object PostgresTransactor {
  def apply[F[_]](
                    config: DatabaseConfig
                  )(implicit
                    F: Async[F],
                    cs: ContextShift[F] 
                  ): Resource[F, HikariTransactor[F]] = {
    for {
      ce <- ExecutionContexts.fixedThreadPool[F](32)
      be <- Blocker[F]
      xa <- HikariTransactor.newHikariTransactor[F](
        "org.postgresql.Driver",
        s"jdbc:postgresql://${config.host}:${config.port}/${config.name}",
        config.user,
        config.password,
        ce,
        be
      )
    } yield xa
  }
}