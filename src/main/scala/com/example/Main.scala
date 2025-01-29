package com.example

import cats.effect.{ExitCode, IO, IOApp}
import cats.implicits._
import fs2.Stream
import com.example.internal.adapter.handler.health.HealthCheckHandler
import com.example.internal.adapter.handler.product.ProductHandler
import com.example.internal.adapter.infrastructure.database.PostgresTransactor
import com.example.internal.adapter.infrastructure.kafka.{KafkaConsumer, KafkaProducer}
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
      (for {
        xa <- PostgresTransactor[IO](config.database)
        eventProducer <- KafkaProducer[IO](config.kafka)
        eventConsumer <- KafkaConsumer[IO](config.kafka)
        redisClient <- RedisClient[IO](config.redis)
      } yield (xa, eventProducer, eventConsumer, redisClient)).use { case (xa, eventProducer, eventConsumer, redisClient) =>

        // Initialize repositories
        val productDbRepo = new ProductDbRepositoryImpl(xa)
        val productCacheRepo = new ProductCacheRepositoryImpl(redisClient)

        // Initialize service with both repositories
        val productEventService = new ProductEventServiceImpl(eventProducer, "product-svc")
        val productService = new ProductServiceImpl(productEventService, productDbRepo, productCacheRepo)

        // Initialize handlers
        val healthCheckHandler = new HealthCheckHandler(xa)
        val productHandler = new ProductHandler(productService)

        // Define Kafka consumer process
        val consumerStream = Stream
          .eval(eventConsumer.consume)
          .repeat
          .compile
          .drain

        // Define HTTP server process
        val httpApp = Router(
          "/api" -> Router(
            "/healthz" -> healthCheckHandler.routes,
            "/products" -> productHandler.routes
          )
        ).orNotFound

        val serverStream = BlazeServerBuilder[IO](serverExecutionContext)
          .bindHttp(config.server.port, config.server.host)
          .withHttpApp(httpApp)
          .serve
          .compile
          .drain

        // Run both processes concurrently
        (consumerStream, serverStream)
          .parMapN((_, _) => ExitCode.Success)
          .handleErrorWith { error =>
            IO.delay(println(s"Error occurred: ${error.getMessage}")) *>
              IO.pure(ExitCode.Error)
          }
      }
    }.handleErrorWith { error =>
      IO.delay(println(s"Application failed to start: ${error.getMessage}")) *>
        IO.pure(ExitCode.Error)
    }
  }
}