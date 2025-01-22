package com.example.internal.core.product.port

import cats.effect.IO
import com.example.internal.core.product.entity.Product
import io.circe.{Decoder, Encoder}

trait ProductCacheRepository {
  def getAll()(implicit decoder: Decoder[List[Product]]): IO[List[Product]]
  def setAll(productList: List[Product], ttl: Int)(implicit encoder: Encoder[List[Product]]): IO[Unit]
  def getById(productId: Long)(implicit decoder: Decoder[Product]): IO[Option[Product]]
  def setById(productId: Long, product: Product, ttl: Int)(implicit encoder: Encoder[Product]): IO[Unit]
}
