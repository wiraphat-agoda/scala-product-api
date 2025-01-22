package com.example.internal.adapter.repository.product.event

import cats.effect.IO
import cats.implicits._
import com.example.internal.adapter.dto.log.LogMessage
import com.example.internal.adapter.dto.product.ProductEvent
import com.example.internal.core.product.port.ProductEventRepository
import com.example.pkg.kafka.MessageBroker
import java.util.UUID

class ProductEventRepositoryImpl(
                                  messageBroker: MessageBroker[IO],
                                  serviceName: String = "product-service"
                                ) extends ProductEventRepository {

  def publish(event: ProductEvent): IO[Unit] = {
    val logMessage = createLogMessage(event)
    messageBroker.sendLog(logMessage)
  }

  private def createLogMessage(event: ProductEvent): LogMessage = {
    val (level, message, error, method, path, statusCode, detail) = event match {
      case ProductEvent.ProductOperationSucceeded(_, detail, _) =>
        ("info",
          s"Product retrieved successfully.",
          None,
          "GET",
          "/api/products",
          201,
          Some(detail)
        )

      case ProductEvent.ProductCreated(product, _) =>
        ("info",
          s"Product created successfully with ID: ${product.id.getOrElse("N/A")}, name: ${product.name}",
          None,
          "POST",
          "/api/products",
          201,
          None
        )

      case ProductEvent.ProductUpdated(product, _) =>
        ("info",
          s"Product updated successfully - ID: ${product.id.getOrElse("N/A")}, name: ${product.name}",
          None,
          "PUT",
          s"/api/products/${product.id.getOrElse("N/A")}",
          200,
          None
        )

      case ProductEvent.ProductDeleted(productId, _) =>
        ("info",
          s"Product deleted successfully - ID: $productId",
          None,
          "DELETE",
          s"/api/products/$productId",
          200,
          None
        )

      case ProductEvent.ProductOperationFailed(operation, productId, errorMsg, _) =>
        ("error",
          s"Product operation '$operation' failed for ID: ${productId.getOrElse("N/A")}",
          Some(errorMsg),
          operation match {
            case "create" => "POST"
            case "update" => "PUT"
            case "delete" => "DELETE"
            case _ => "GET"
          },
          s"/api/products${productId.map(id => s"/$id").getOrElse("")}",
          500,
          None
        )
    }
    val logMsg = LogMessage(
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
    println(logMsg)

    logMsg
  }
}