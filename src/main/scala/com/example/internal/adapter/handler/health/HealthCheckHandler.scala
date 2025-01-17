package com.example.internal.adapter.handler.health

import cats.effect.IO
import com.example.BuildInfo
import com.example.internal.adapter.dto.health.{DatabaseStatus, HealthStatusDTO}
import doobie.implicits._
import doobie.util.transactor.Transactor
import org.http4s.HttpRoutes
import org.http4s.dsl.io._
import io.circe.generic.auto._
import org.http4s.circe.CirceEntityEncoder._
import org.http4s.server.Router

import java.time.OffsetDateTime

class HealthCheckHandler(xa: Transactor[IO]) {
  private def getCurrentTimestamp: String =
    OffsetDateTime.now().toString

  private def checkDatabase: IO[DatabaseStatus] = {
    for {
      start <- IO(System.currentTimeMillis())
      _ <- sql"SELECT 1".query[Int].unique.transact(xa)
      end <- IO(System.currentTimeMillis())
    } yield DatabaseStatus(
      status = "healthy",
      latency = end - start
    )
  }

  val routes: HttpRoutes[IO] = HttpRoutes.of[IO] {
    case GET -> Root =>
      for {
        dbStatus <- checkDatabase
        status = HealthStatusDTO(
          status = "healthy",
          version = BuildInfo.version,
          timestamp = getCurrentTimestamp,
          database = dbStatus
        )
        response <- Ok(status)
      } yield response
  }

  val healthRouter: HttpRoutes[IO] = Router(
    "/healthz" -> this.routes
  )
}