package com.example.internal.adapter.repository.product

import cats.effect.IO
import com.example.internal.core.product.entity.Product
import com.example.internal.core.product.port.ProductRepository
import doobie._
import doobie.implicits._
import doobie.postgres.implicits._

class ProductRepositoryImpl(xa: Transactor[IO]) extends ProductRepository {
  private val tableName = "products"

  import Product._

  def create(product: Product): IO[Product] = {
    sql"""
      INSERT INTO products (name, description, price)
      VALUES (${product.name}, ${product.description}, ${product.price})
      RETURNING id, name, description, price
    """.query[Product].unique.transact(xa)
  }

  def getById(id: Long): IO[Option[Product]] = {
    sql"""
      SELECT id, name, description, price
      FROM products
      WHERE id = $id
    """.query[Product].option.transact(xa)
  }

  def getAll: IO[List[Product]] = {
    sql"""
      SELECT id, name, description, price
      FROM products
    """.query[Product].to[List].transact(xa)
  }

  def update(id: Long, product: Product): IO[Option[Product]] = {
    sql"""
      UPDATE products
      SET name = ${product.name},
          description = ${product.description},
          price = ${product.price}
      WHERE id = $id
      RETURNING id, name, description, price
    """.query[Product].option.transact(xa)
  }

  def delete(id: Long): IO[Boolean] = {
    sql"""
      DELETE FROM products
      WHERE id = $id
    """.update.run.map(_ > 0).transact(xa)
  }
}

// val nums = List(1,3,5)
// val transformer: List[Int] = (num int) => List(num, 2 * num)
// val transformed: List[Int] = nums.flatMap(transformer)   // List(1, 2, 3, 6, 5, 10)