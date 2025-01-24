package com.example.internal.core.product.port

import cats.effect.IO
import com.example.internal.adapter.dto.event.ProductEvent

trait ProductEventService {
  def publish(event: ProductEvent): IO[Unit]
}
