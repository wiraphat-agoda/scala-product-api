package com.example.internal.adapter.infrastructure.kafka

import com.example.internal.adapter.dto.log.LogMessage

trait EventProducer[F[_]] {
  def sendLog(logMessage: LogMessage): F[Unit]
}
