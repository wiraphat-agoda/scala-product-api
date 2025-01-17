package com.example

import com.example.pkg.database.DatabaseTransactor
import com.example.internal.adapter.handler.product.ProductHandler
import com.example.internal.adapter.repository.product.ProductRepositoryImpl
import com.example.internal.core.product.service.ProductServiceImpl
import cats.effect.{ExitCode, IO, IOApp}
import com.example.internal.adapter.handler.health.HealthCheckHandler
import org.http4s.server.blaze.BlazeServerBuilder
import org.http4s.implicits._
import com.example.pkg.config.AppConfig
import org.http4s.server.Router

import scala.concurrent.ExecutionContext

object Main extends IOApp {
  private val serverExecutionContext = ExecutionContext.global
  private val appConfig: IO[AppConfig] = AppConfig.getInstance

  def run(args: List[String]): IO[ExitCode] = {
    appConfig.flatMap { config =>
      DatabaseTransactor.create(config).use { xa =>

        val healthCheckHandler = new HealthCheckHandler(xa)

        val productRepo = new ProductRepositoryImpl(xa)
        val productService = new ProductServiceImpl(productRepo)
        val productHandler = new ProductHandler(productService)

        val httpHandlers = Router(
          "/" -> healthCheckHandler.healthRouter,
          "/api" -> productHandler.productRouter
        ).orNotFound

        BlazeServerBuilder[IO](serverExecutionContext)
          .bindHttp(config.server.port, config.server.host)
          .withHttpApp(httpHandlers)
          .serve
          .compile
          .drain
          .as(ExitCode.Success)
      }
    }
  }
}