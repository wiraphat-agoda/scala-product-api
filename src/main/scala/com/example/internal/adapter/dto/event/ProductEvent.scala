package com.example.internal.adapter.dto.event

import java.time.OffsetDateTime

case class ProductEvent(
                          httpMethod: String, // GET , POST , PUT , DELETE
                          success: Boolean, // true , false
                          message: String,  // error message
                          detail: String,  // ex. db_found , cache_hit , cache_miss
                          timestamp: OffsetDateTime,
                          productId: Option[Long]
                          )
