package com.example

import cats.effect.{ExitCode, IO, IOApp}
import com.example.internal.adapter.handler.health.HealthCheckHandler
import com.example.internal.adapter.handler.product.ProductHandler
import com.example.internal.adapter.infrastructure.database.PostgresTransactor
import com.example.internal.adapter.infrastructure.kafka.KafkaProducer
import com.example.internal.adapter.infrastructure.redis.RedisClient
import com.example.internal.adapter.repository.product.cache.ProductCacheRepositoryImpl
import com.example.internal.adapter.repository.product.db.ProductDbRepositoryImpl
import com.example.internal.core.product.service.{ProductEventServiceImpl, ProductServiceImpl}
import com.example.pkg.config.AppConfig
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
        xa <- PostgresTransactor[IO](config.database)
        eventProducer <- KafkaProducer[IO](config.kafka)
        redisClient <- RedisClient[IO](config.redis)
      } yield (xa, eventProducer, redisClient)).use { case (xa, eventProducer, redisClient) =>

        // Initialize repositories
        val productDbRepo = new ProductDbRepositoryImpl(xa)
        val productCacheRepo = new ProductCacheRepositoryImpl(redisClient)

        // Initialize service with both repositories
        val productEventService = new ProductEventServiceImpl(eventProducer, "product-svc")
        val productService = new ProductServiceImpl(productEventService, productDbRepo, productCacheRepo)

        // Initialize handlers
        val healthCheckHandler = new HealthCheckHandler(xa)
        val productHandler = new ProductHandler(productService) // HTTP handler

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