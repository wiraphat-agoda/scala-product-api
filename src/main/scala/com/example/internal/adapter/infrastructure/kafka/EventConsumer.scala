package com.example.internal.adapter.infrastructure.kafka

trait EventConsumer[F[_]] {
  def consume(): F[Unit]
}
