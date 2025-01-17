package com.example.internal.core.product.entity

import doobie.Read
import io.circe.generic.semiauto._
import io.circe.{Decoder, Encoder}

case class Product(
                    id: Option[Long],
                    name: String,
                    description: String,
                    price: BigDecimal
                  )

object Product {
  implicit val encoder: Encoder[Product] = deriveEncoder[Product]
  implicit val decoder: Decoder[Product] = deriveDecoder[Product]

  implicit val productRead: Read[Product] =
    Read[(Option[Long], String, String, BigDecimal)].map {
      case (id, name, description, price) =>
        Product(id, name, description, price)
    }
}
