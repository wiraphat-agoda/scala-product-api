package com.example

import cats.effect.{ExitCode, IO, IOApp}
import com.example.internal.adapter.handler.health.HealthCheckHandler
import com.example.internal.adapter.handler.product.ProductHandler
import com.example.internal.adapter.repository.product.db.ProductDbRepositoryImpl
import com.example.internal.adapter.repository.product.event.ProductEventRepositoryImpl
import com.example.internal.core.product.service.ProductServiceImpl
import com.example.pkg.config.AppConfig
import com.example.pkg.database.DatabaseTransactor
import com.example.pkg.kafka.KafkaProducerClient
import org.http4s.implicits._
import org.http4s.server.Router
import org.http4s.server.blaze.BlazeServerBuilder

import scala.concurrent.ExecutionContext

object Main extends IOApp {
  private val serverExecutionContext = ExecutionContext.global
  private val appConfig: IO[AppConfig] = AppConfig.getInstance

  def run(args: List[String]): IO[ExitCode] = {
    appConfig.flatMap { config =>
      // Create resources for both Database and Kafka
      (for {
        xa <- DatabaseTransactor.create(config)
        messageBroker <- KafkaProducerClient[IO](config.kafka)
      } yield (xa, messageBroker)).use { case (xa, messageBroker) =>

        // Initialize repositories
        val productDbRepo = new ProductDbRepositoryImpl(xa)
        val productEventRepo = new ProductEventRepositoryImpl(
          messageBroker = messageBroker,
          serviceName = "product-service"
        )

        // Initialize service with both repositories
        val productService = new ProductServiceImpl(productDbRepo, productEventRepo)

        // Initialize handlers
        val healthCheckHandler = new HealthCheckHandler(xa)
        val productHandler = new ProductHandler(productService)

        // Combine routes
        val httpHandlers = Router(
          "/" -> healthCheckHandler.healthRouter,
          "/api" -> productHandler.productRouter
        ).orNotFound

        // Start HTTP server
        BlazeServerBuilder[IO](serverExecutionContext)
          .bindHttp(config.server.port, config.server.host)
          .withHttpApp(httpHandlers)
          .serve
          .compile
          .drain
          .as(ExitCode.Success)
      }
    }.handleErrorWith { error =>
      IO.delay(println(s"Application failed to start: ${error.getMessage}")) *>
        IO.pure(ExitCode.Error)
    }
  }
}