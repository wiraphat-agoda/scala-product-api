package com.example.internal.core.product.service

import cats.effect.IO
import com.example.internal.core.product.entity.Product
import com.example.internal.core.product.port.{ProductRepository, ProductService}

class ProductServiceImpl(repository: ProductRepository) extends ProductService {
  def createProduct(product: Product): IO[Product] =
    repository.create(product)

  def getProduct(id: Long): IO[Option[Product]] =
    repository.getById(id)

  def getAllProducts: IO[List[Product]] =
    repository.getAll

  def updateProduct(id: Long, product: Product): IO[Option[Product]] =
    repository.update(id, product)

  def deleteProduct(id: Long): IO[Boolean] =
    repository.delete(id)
}
