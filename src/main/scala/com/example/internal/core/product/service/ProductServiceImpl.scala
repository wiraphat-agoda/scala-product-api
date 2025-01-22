// com/example/internal/core/product/service/ProductServiceImpl.scala
package com.example.internal.core.product.service

import cats.effect.IO
import cats.implicits._
import com.example.internal.adapter.dto.product.ProductEvent
import com.example.internal.core.product.entity.Product
import com.example.internal.core.product.port.{ProductCacheRepository, ProductDbRepository, ProductEventRepository, ProductService}

class ProductServiceImpl(
                          productDbRepo: ProductDbRepository,
                          productEventRepo: ProductEventRepository,
                          productCacheRepo: ProductCacheRepository
                        ) extends ProductService {

  def createProduct(product: Product): IO[Product] = {
    (for {
      createdProduct <- productDbRepo.create(product)
      _ <- productEventRepo.publish(ProductEvent.ProductCreated(createdProduct))
    } yield createdProduct)
      .handleErrorWith { error =>
        productEventRepo.publish(
          ProductEvent.ProductOperationFailed("create", None, error.getMessage)
        ) *> IO.raiseError(error)
      }
  }

  def getProduct(id: Long): IO[Option[Product]] = {
    (for {
      cachedProduct <- productCacheRepo.getById(id)
      product <- cachedProduct match {
        case Some(p) => IO.pure(Some(p))
        case None => for {
          dbProduct <- productDbRepo.getById(id)

          _ <- dbProduct.traverse(p => productCacheRepo.setById(id, p, 3600))
        } yield dbProduct
      }
    } yield product)
      .handleErrorWith { error =>
        productEventRepo.publish(
          ProductEvent.ProductOperationFailed("get", Some(id), error.getMessage)
        ) *> IO.raiseError(error)
      }
  }

  def getAllProducts: IO[List[Product]] = {
    (for {
      // Try to get from cache first
      cachedProducts <- productCacheRepo.getAll()

      products <- if (cachedProducts.nonEmpty) {
        // If found in cache, return cached data and publish event
        productEventRepo.publish(
          ProductEvent.ProductOperationSucceeded(None, "cache_hit")
        ) *> IO.pure(cachedProducts)
      } else {
        // If not in cache, get from DB
        for {
          // Publish cache miss event
          _ <- productEventRepo.publish(
            ProductEvent.ProductOperationSucceeded(None, "cache_miss")
          )
          // Get from DB
          dbProducts <- productDbRepo.getAll
          // Cache the products if found
          _ <- if (dbProducts.nonEmpty) {
            productCacheRepo.setAll(dbProducts, 3600) *> // Cache for 1 hour
              productEventRepo.publish(
                ProductEvent.ProductOperationSucceeded(None, "db_found")
              )
          } else {
            productEventRepo.publish(
              ProductEvent.ProductOperationSucceeded(None, "db_empty")
            )
          }
        } yield dbProducts
      }
    } yield products)
      .handleErrorWith { error =>
        productEventRepo.publish(
          ProductEvent.ProductOperationFailed("getAll", None, error.getMessage)
        ) *> IO.raiseError(error)
      }
  }

  def updateProduct(id: Long, product: Product): IO[Option[Product]] = {
    (for {
      maybeUpdated <- productDbRepo.update(id, product)
      _ <- maybeUpdated.traverse(updated =>
        productEventRepo.publish(ProductEvent.ProductUpdated(updated))
      )
    } yield maybeUpdated)
      .handleErrorWith { error =>
        productEventRepo.publish(
          ProductEvent.ProductOperationFailed("update", Some(id), error.getMessage)
        ) *> IO.raiseError(error)
      }
  }

  def deleteProduct(id: Long): IO[Boolean] = {
    (for {
      deleted <- productDbRepo.delete(id)
      _ <- if (deleted) productEventRepo.publish(ProductEvent.ProductDeleted(id))
      else IO.unit
    } yield deleted)
      .handleErrorWith { error =>
        productEventRepo.publish(
          ProductEvent.ProductOperationFailed("delete", Some(id), error.getMessage)
        ) *> IO.raiseError(error)
      }
  }
}