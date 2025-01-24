package com.example.internal.core.product.service

import cats.effect.IO
import cats.implicits._
import com.example.constant.HttpMethod
import com.example.internal.adapter.dto.event.ProductEvent
import com.example.internal.core.product.entity.Product
import com.example.internal.core.product.port.{ProductCacheRepository, ProductDbRepository, ProductEventService, ProductService}

import java.time.OffsetDateTime
import scala.concurrent.duration._

class ProductServiceImpl(
                          productEventSvc: ProductEventService,
                          productDbRepo: ProductDbRepository,
                          productCacheRepo: ProductCacheRepository
                        ) extends ProductService {

  private val DefaultCacheTTL = 3600 // 1 hour in seconds
  private val InvalidationTTL = 1 // 1 second for invalidated entries

  // Empty values for invalidation
  private val EmptyProduct = Product(None, "", "", BigDecimal(0))
  private val EmptyProductList = List.empty[Product]

  private def invalidateById(id: Long): IO[Unit] = {
    productCacheRepo.setById(id, EmptyProduct, InvalidationTTL)
  }

  private def invalidateAll(): IO[Unit] = {
    productCacheRepo.setAll(EmptyProductList, InvalidationTTL)
  }

  private def createSuccessEvent(
                                  method: String,
                                  productId: Option[Long],
                                  detail: String
                                ): ProductEvent = {
    ProductEvent(
      httpMethod = method,
      success = true,
      message = "",
      detail = detail,
      timestamp = OffsetDateTime.now(),
      productId = productId
    )
  }

  private def createErrorEvent(
                                method: String,
                                productId: Option[Long],
                                errorMessage: String
                              ): ProductEvent = {
    ProductEvent(
      httpMethod = method,
      success = false,
      message = errorMessage,
      detail = "error",
      timestamp = OffsetDateTime.now(),
      productId = productId
    )
  }

  def createProduct(product: Product): IO[Product] = {
    (for {
      // Create in DB first
      createdProduct <- productDbRepo.create(product)
      // Update cache (write-through)
      _ <- productCacheRepo.setById(createdProduct.id.get, createdProduct, DefaultCacheTTL)
      // Invalidate the all-products cache
      _ <- invalidateAll()
      // Publish event
      _ <- productEventSvc.publish(createSuccessEvent(
        HttpMethod.POST,
        Some(createdProduct.id.get),
        "db_created"
      ))
    } yield createdProduct)
      .handleErrorWith { error =>
        productEventSvc.publish(
          createErrorEvent(HttpMethod.POST, None, error.getMessage)
        ) *> IO.raiseError(error)
      }
  }

  def getProduct(id: Long): IO[Option[Product]] = {
    (for {
      cachedProduct <- productCacheRepo.getById(id)
      product <- cachedProduct match {
        case Some(p) if p == EmptyProduct =>
          // Found invalidated entry
          productEventSvc.publish(
            createSuccessEvent(HttpMethod.GET, Some(id), "cache_invalidated")
          ) *> productDbRepo.getById(id).flatMap {
            case Some(dbProduct) =>
              productCacheRepo.setById(id, dbProduct, DefaultCacheTTL) *>
                productEventSvc.publish(
                  createSuccessEvent(HttpMethod.GET, Some(id), "db_found")
                ) *>
                IO.pure(Some(dbProduct))
            case None =>
              invalidateById(id) *>
                productEventSvc.publish(
                  createSuccessEvent(HttpMethod.GET, Some(id), "not_found")
                ) *>
                IO.pure(None)
          }
        case Some(p) =>
          // Cache hit
          productEventSvc.publish(
            createSuccessEvent(HttpMethod.GET, Some(id), "cache_hit")
          ) *> IO.pure(Some(p))
        case None =>
          // Cache miss
          for {
            _ <- productEventSvc.publish(
              createSuccessEvent(HttpMethod.GET, Some(id), "cache_miss")
            )
            dbProduct <- productDbRepo.getById(id)
            _ <- dbProduct match {
              case Some(p) =>
                productCacheRepo.setById(id, p, DefaultCacheTTL) *>
                  productEventSvc.publish(
                    createSuccessEvent(HttpMethod.GET, Some(id), "db_found")
                  )
              case None =>
                invalidateById(id) *>
                  productEventSvc.publish(
                    createSuccessEvent(HttpMethod.GET, Some(id), "not_found")
                  )
            }
          } yield dbProduct
      }
    } yield product)
      .handleErrorWith { error =>
        productEventSvc.publish(
          createErrorEvent(HttpMethod.GET, Some(id), error.getMessage)
        ) *> IO.raiseError(error)
      }
  }

  def getAllProducts: IO[List[Product]] = {
    (for {
      cachedProducts <- productCacheRepo.getAll()
      products <- if (cachedProducts == EmptyProductList) {
        // Found invalidated list
        for {
          _ <- productEventSvc.publish(
            createSuccessEvent(HttpMethod.GET, None, "cache_invalidated")
          )
          dbProducts <- productDbRepo.getAll
          _ <- if (dbProducts.nonEmpty) {
            productCacheRepo.setAll(dbProducts, DefaultCacheTTL) *>
              productEventSvc.publish(
                createSuccessEvent(HttpMethod.GET, None, "db_found")
              )
          } else {
            invalidateAll() *>
              productEventSvc.publish(
                createSuccessEvent(HttpMethod.GET, None, "db_empty")
              )
          }
        } yield dbProducts
      } else if (cachedProducts.nonEmpty) {
        productEventSvc.publish(
          createSuccessEvent(HttpMethod.GET, None, "cache_hit")
        ) *> IO.pure(cachedProducts)
      } else {
        for {
          _ <- productEventSvc.publish(
            createSuccessEvent(HttpMethod.GET, None, "cache_miss")
          )
          dbProducts <- productDbRepo.getAll
          _ <- if (dbProducts.nonEmpty) {
            productCacheRepo.setAll(dbProducts, DefaultCacheTTL) *>
              productEventSvc.publish(
                createSuccessEvent(HttpMethod.GET, None, "db_found")
              )
          } else {
            invalidateAll() *>
              productEventSvc.publish(
                createSuccessEvent(HttpMethod.GET, None, "db_empty")
              )
          }
        } yield dbProducts
      }
    } yield products)
      .handleErrorWith { error =>
        productEventSvc.publish(
          createErrorEvent(HttpMethod.GET, None, error.getMessage)
        ) *> IO.raiseError(error)
      }
  }

  def updateProduct(id: Long, product: Product): IO[Option[Product]] = {
    (for {
      maybeUpdated <- productDbRepo.update(id, product)
      _ <- maybeUpdated.traverse { updated =>
        for {
          // Update individual cache entry
          _ <- productCacheRepo.setById(id, updated, DefaultCacheTTL)
          // Invalidate all-products cache
          _ <- invalidateAll()
          // Publish event
          _ <- productEventSvc.publish(
            createSuccessEvent(HttpMethod.PUT, Some(id), "db_updated")
          )
        } yield ()
      }
    } yield maybeUpdated)
      .handleErrorWith { error =>
        productEventSvc.publish(
          createErrorEvent(HttpMethod.PUT, Some(id), error.getMessage)
        ) *> IO.raiseError(error)
      }
  }

  def deleteProduct(id: Long): IO[Boolean] = {
    (for {
      deleted <- productDbRepo.delete(id)
      _ <- if (deleted) {
        for {
          // Invalidate individual cache entry
          _ <- invalidateById(id)
          // Invalidate all-products cache
          _ <- invalidateAll()
          // Publish event
          _ <- productEventSvc.publish(
            createSuccessEvent(HttpMethod.DELETE, Some(id), "db_deleted")
          )
        } yield ()
      } else {
        productEventSvc.publish(
          createSuccessEvent(HttpMethod.DELETE, Some(id), "not_found")
        )
      }
    } yield deleted)
      .handleErrorWith { error =>
        productEventSvc.publish(
          createErrorEvent(HttpMethod.DELETE, Some(id), error.getMessage)
        ) *> IO.raiseError(error)
      }
  }
}