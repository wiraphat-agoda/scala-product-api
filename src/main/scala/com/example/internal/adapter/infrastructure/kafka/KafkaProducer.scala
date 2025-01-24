package com.example.internal.adapter.infrastructure.kafka

import cats.effect.{Resource, Sync, Timer}
import cats.implicits._
import com.example.internal.adapter.dto.log.LogMessage
import com.example.pkg.config.KafkaConfig
import io.circe.syntax._
import org.apache.kafka.clients.producer.{KafkaProducer, ProducerConfig, ProducerRecord}
import org.apache.kafka.common.serialization.StringSerializer

import java.util.Properties
import scala.concurrent.duration._
import scala.jdk.CollectionConverters._

object KafkaProducer {
  def apply[F[_]: Sync: Timer](config: KafkaConfig): Resource[F, EventProducer[F]] = {
    def createProducer: F[KafkaProducer[String, String]] = Sync[F].delay {
      val props = new Properties()
      props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, config.bootstrapServers)
      props.put(ProducerConfig.CLIENT_ID_CONFIG, config.clientId)
      props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, classOf[StringSerializer].getName)
      props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, classOf[StringSerializer].getName)
      props.put(ProducerConfig.ACKS_CONFIG, config.acks)
      props.put(ProducerConfig.RETRIES_CONFIG, config.retries.toString)
      props.put(ProducerConfig.BATCH_SIZE_CONFIG, config.batchSize.toString)
      props.put(ProducerConfig.LINGER_MS_CONFIG, config.lingerMs.toString)
      props.put(ProducerConfig.BUFFER_MEMORY_CONFIG, config.bufferMemory.toString)

      new KafkaProducer[String, String](props)
    }

    Resource.make(createProducer)(producer => Sync[F].delay(producer.close())).map { producer =>
      new EventProducer[F] {
        def sendLog(logMessage: LogMessage): F[Unit] = {
          Sync[F].delay {
            val record = new ProducerRecord[String, String](
              config.topic,
              logMessage.requestId.getOrElse("default"),
              logMessage.asJson.noSpaces
            )
            producer.send(record)
          }.void
        }
      }
    }
  }
}