package com.example.internal.core.product.port

import cats.effect.IO
import com.example.internal.adapter.dto.product.ProductEvent

trait ProductEventRepository {
  def publish(event: ProductEvent): IO[Unit]
}
