package com.example.pkg.redis

import cats.effect.{Blocker, ContextShift, IO, Resource}
import redis.clients.jedis.{Jedis, JedisPool, JedisPoolConfig}

class RedisClient private (jedisPool: JedisPool) {

  def use[T](operation: Jedis => T): IO[T] = {
    IO {
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
  def apply(
              host: String = "localhost",
              port: Int = 6379,
              maxTotal: Int = 10,
              maxIdle: Int = 5,
              minIdle: Int = 1
            )(implicit cs: ContextShift[IO]): Resource[IO, RedisClient] = {
    Resource.make {
      IO {
        val poolConfig = new JedisPoolConfig()
        poolConfig.setMaxTotal(maxTotal)
        poolConfig.setMaxIdle(maxIdle)
        poolConfig.setMinIdle(minIdle)

        new JedisPool(poolConfig, host, port)
      }
    } { pool =>
      IO {
        pool.close()
      }
    }.map(new RedisClient(_))
  }
}
