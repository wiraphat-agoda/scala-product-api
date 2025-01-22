package com.example.internal.adapter.dto.product

import java.time.OffsetDateTime
import com.example.internal.core.product.entity.Product
import io.circe.{Encoder, Decoder}
import io.circe.generic.semiauto._

sealed trait ProductEvent {
  def timestamp: OffsetDateTime
  def productId: Option[Long]
}

object ProductEvent {
  case class ProductCreated(
                             product: Product,
                             timestamp: OffsetDateTime = OffsetDateTime.now()
                           ) extends ProductEvent {
    def productId: Option[Long] = product.id
  }

  case class ProductUpdated(
                             product: Product,
                             timestamp: OffsetDateTime = OffsetDateTime.now()
                           ) extends ProductEvent {
    def productId: Option[Long] = product.id
  }

  case class ProductDeleted(
                             id: Long,
                             timestamp: OffsetDateTime = OffsetDateTime.now()
                           ) extends ProductEvent {
    def productId: Option[Long] = Some(id)
  }

  case class ProductOperationFailed(
                                     operation: String,
                                     productId: Option[Long],
                                     error: String,
                                     timestamp: OffsetDateTime = OffsetDateTime.now()
                                   ) extends ProductEvent

  case class ProductOperationSucceeded(
                                      productId: Option[Long],
                                      detail: String,
                                      timestamp: OffsetDateTime = OffsetDateTime.now()
                                      ) extends ProductEvent

  implicit val encoder: Encoder[ProductEvent] = deriveEncoder[ProductEvent]
  implicit val decoder: Decoder[ProductEvent] = deriveDecoder[ProductEvent]
}