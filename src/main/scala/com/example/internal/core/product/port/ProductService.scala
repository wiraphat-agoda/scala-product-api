package com.example.internal.core.product.port

import cats.effect.IO
import com.example.internal.core.product.entity.Product

trait ProductService {
  def createProduct(product: Product): IO[Product]
  def getProduct(id: Long): IO[Option[Product]]
  def getAllProducts: IO[List[Product]]
  def updateProduct(id: Long, product: Product): IO[Option[Product]]
  def deleteProduct(id: Long): IO[Boolean]
}
