package com.example.internal.core.product.port

import cats.effect.IO
import com.example.internal.core.product.entity.Product

trait ProductDbRepository {
  def create(product: Product): IO[Product]
  def getById(id: Long): IO[Option[Product]]
  def getAll: IO[List[Product]]
  def update(id: Long, product: Product): IO[Option[Product]]
  def delete(id: Long): IO[Boolean]
}