package com.example.internal.adapter.infrastructure.redis

import cats.effect.{Sync, Resource}
import com.example.pkg.config.RedisConfig
import redis.clients.jedis.{Jedis, JedisPool, JedisPoolConfig}

class RedisClient[F[_]] private (jedisPool: JedisPool)(implicit F: Sync[F]) {

  def use[T](operation: Jedis => T): F[T] = {
    F.delay {
      val jedis = jedisPool.getResource
      try {
        operation(jedis)
      } finally {
        jedis.close()
      }
    }
  }
}

object RedisClient {
  private val maxTotal: Int = 10
  private val maxIdle: Int = 5
  private val minIdle: Int = 1

  def apply[F[_]](
                   config: RedisConfig
                 )(implicit F: Sync[F]): Resource[F, RedisClient[F]] = {
    Resource.make {
      F.delay {
        val poolConfig = new JedisPoolConfig()
        poolConfig.setMaxTotal(maxTotal)
        poolConfig.setMaxIdle(maxIdle)
        poolConfig.setMinIdle(minIdle)

        new JedisPool(poolConfig, config.host, config.port)
      }
    } { pool =>
      F.delay {
        pool.close()
      }
    }.map(new RedisClient[F](_))
  }
}