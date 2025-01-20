// com/example/internal/core/product/service/ProductServiceImpl.scala
package com.example.internal.core.product.service

import cats.effect.IO
import cats.implicits._
import com.example.internal.adapter.dto.product.ProductEvent
import com.example.internal.core.product.entity.Product
import com.example.internal.core.product.port.{ProductDbRepository, ProductEventRepository, ProductService}

class ProductServiceImpl(
                          productDbRepo: ProductDbRepository,
                          productEventRepo: ProductEventRepository
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
    productDbRepo.getById(id).handleErrorWith { error =>
      productEventRepo.publish(
        ProductEvent.ProductOperationFailed("get", Some(id), error.getMessage)
      ) *> IO.raiseError(error)
    }
  }

  def getAllProducts: IO[List[Product]] = {
    productDbRepo.getAll.handleErrorWith { error =>
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