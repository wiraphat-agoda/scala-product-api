package com.example.internal.adapter.repository.product.cache

import cats.effect.IO
import com.example.pkg.redis.RedisClient
import com.example.internal.core.product.entity.Product
import com.example.internal.core.product.port.ProductCacheRepository
import io.circe.{Decoder, Encoder}
import io.circe.parser.decode
import io.circe.syntax._

class ProductCacheRepositoryImpl(redisClient: RedisClient) extends ProductCacheRepository{
  def getAll()(implicit decoder: Decoder[List[Product]]): IO[List[Product]] = {
    redisClient.use(jedis => {
      val productListStr = Option(jedis.get("product:all"))
      val productList = productListStr match {
        case Some(str) => decode[List[Product]](str).getOrElse(List.empty)
        case None => List.empty
      }

      productList
    })
  }
  def setAll(productList: List[Product], ttl: Int)(implicit encoder: Encoder[List[Product]]): IO[Unit] = {
    redisClient.use(jedis => {
      jedis.setex(s"product:all", ttl, productList.asJson.noSpaces)
    })
  }
  def getById(productId: Long)(implicit decoder: Decoder[Product]): IO[Option[Product]] = {
    redisClient.use(jedis => {
      for {
        productStr <- Option(jedis.get(s"product:$productId"))
        product <- decode[Product](productStr).toOption
      } yield product
    })
  }

  def setById(productId: Long, product: Product, ttl: Int)(implicit encoder: Encoder[Product]): IO[Unit] = {
    redisClient.use(jedis => {
      for {
        _ <- jedis.setex(s"product:$productId", ttl, product.asJson.noSpaces)
      } yield ()
    })
  }

  def delAll(): IO[Unit] = {
    redisClient.use(jedis =>
      for {
        keys <- IO(jedis.keys("product:*"))
        _ <- if (keys.isEmpty) {
          IO.unit
        } else {
          IO(jedis.del(keys.toArray(new Array[String](keys.size)): _*)).void
        }
      } yield ()
    )
  }

  def delById(productId: Long): IO[Unit] = {
    redisClient.use(jedis => {
      IO(jedis.del("product:$productId")).void
    })
  }
}
