package com.example.internal.adapter.dto.log

import java.time.OffsetDateTime
import io.circe.{Encoder, Decoder}
import io.circe.generic.semiauto._

case class LogMessage(
                       timestamp: OffsetDateTime,
                       level: String,
                       service: String,
                       message: String,
                       requestId: Option[String],
                       method: Option[String],
                       path: Option[String],
                       statusCode: Option[Int],
                       duration: Option[Double],
                       userId: Option[String],
                       error: Option[String]
                     )

object LogMessage {
  implicit val encoder: Encoder[LogMessage] = deriveEncoder[LogMessage]
  implicit val decoder: Decoder[LogMessage] = deriveDecoder[LogMessage]
}