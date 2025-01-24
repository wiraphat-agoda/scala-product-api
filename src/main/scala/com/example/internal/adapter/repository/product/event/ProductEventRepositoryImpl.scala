package com.example.internal.adapter.repository.product.event

import cats.effect.IO
import cats.implicits._
import com.example.internal.adapter.constant.http.HttpMethod
import com.example.internal.adapter.dto.log.LogMessage
import com.example.internal.adapter.dto.product.{ProductEvent, ProductEventDTO}
import com.example.internal.core.product.port.ProductEventRepository
import com.example.pkg.kafka.MessageBroker

import java.util.UUID

class ProductEventRepositoryImpl(
                                  messageBroker: MessageBroker[IO],
                                  serviceName: String = "product-service"
                                ) extends ProductEventRepository {

  def publish(event: ProductEventDTO): IO[Unit] = {
    val logMessage = createLogMessage(event)
    messageBroker.sendLog(logMessage)
  }

  private def createLogMessage(event: ProductEventDTO): LogMessage = {
    val (level, message, error, method, path, statusCode, detail) = event match {
      // GET - All products (without productId)
      case ProductEventDTO(HttpMethod.GET, true, _, detail, _, None) =>
        ("info",
          "Products retrieved successfully.",
          None,
          HttpMethod.GET,
          "/api/products",
          200,
          Some(detail)
        )

      // GET - Single product (with productId)
      case ProductEventDTO(HttpMethod.GET, true, _, detail, _, Some(pid)) =>
        ("info",
          s"Product ID:$pid retrieved successfully.",
          None,
          HttpMethod.GET,
          s"/api/products/$pid",
          200,
          Some(detail)
        )

      // POST - Always should have productId after successful creation
      case ProductEventDTO(HttpMethod.POST, true, _, detail, _, Some(pid)) =>
        ("info",
          s"Product ID:$pid created successfully.",
          None,
          HttpMethod.POST,
          "/api/products",
          201,
          Some(detail)
        )

      // PUT - Must have productId
      case ProductEventDTO(HttpMethod.PUT, true, _, detail, _, Some(pid)) =>
        ("info",
          s"Product ID:$pid updated successfully.",
          None,
          HttpMethod.PUT,
          s"/api/products/$pid",
          200,
          Some(detail)
        )

      // DELETE - Must have productId
      case ProductEventDTO(HttpMethod.DELETE, true, _, detail, _, Some(pid)) =>
        ("info",
          s"Product ID:$pid deleted successfully.",
          None,
          HttpMethod.DELETE,
          s"/api/products/$pid",
          200,
          Some(detail)
        )

      // Error cases with specific error messages
      case ProductEventDTO(method, false, errorMsg, detail, _, maybeId) =>
        val baseMessage = method match {
          case HttpMethod.GET =>
            maybeId.map(pid => s"Product ID:$pid").getOrElse("All products") + " retrieved fail."
          case HttpMethod.POST => "Product creation failed."
          case HttpMethod.PUT =>
            maybeId.map(pid => s"Product ID:$pid update failed.").getOrElse("Product update failed: No ID provided.")
          case HttpMethod.DELETE =>
            maybeId.map(pid => s"Product ID:$pid deletion failed.").getOrElse("Product deletion failed: No ID provided.")
          case _ => "Unknown operation failed."
        }

        ("error",
          baseMessage,
          Some(errorMsg),  // Using the actual error message from DTO
          method,
          maybeId.fold("/api/products")(pid => s"/api/products/$pid"),
          500,
          Some(detail)
        )

      // Catch-all case for unexpected patterns
      case ProductEventDTO(method, _, msg, detail, _, _) =>
        ("error",
          "Unexpected event pattern",
          Some(msg),
          method,
          "/api/products",
          500,
          Some(detail)
        )
    }

    LogMessage(
      timestamp = event.timestamp,
      level = level,
      service = serviceName,
      message = message,
      requestId = Some(UUID.randomUUID().toString),
      method = Some(method),
      path = Some(path),
      statusCode = Some(statusCode),
      duration = None,
      userId = None,
      error = error,
      detail = detail
    )
  }
}