package com.example.internal.adapter.dto.health

case class HealthStatusDTO(
                         status: String,
                         version: String,
                         timestamp: String,
                         database: DatabaseStatus
                       )

case class DatabaseStatus(
                           status: String,
                           latency: Long
                         )