package com.example.internal.adapter.infrastructure.kafka

import cats.effect.{Concurrent, ContextShift, Resource, Timer}
import cats.implicits._
import com.example.pkg.config.KafkaConfig
import org.apache.kafka.clients.consumer.{ConsumerConfig, KafkaConsumer => JKafkaConsumer}
import org.apache.kafka.common.serialization.StringDeserializer

import java.util.Properties
import scala.concurrent.duration._
import scala.jdk.CollectionConverters._

object KafkaConsumer {
  def apply[F[_]](
                   config: KafkaConfig
                 )(implicit F: Concurrent[F], timer: Timer[F], cs: ContextShift[F]): Resource[F, EventConsumer[F]] = {

    def createConsumer: F[JKafkaConsumer[String, String]] = F.delay {
      val props = new Properties()

      // Common configurations
      props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, config.bootstrapServers)
      props.put(ConsumerConfig.CLIENT_ID_CONFIG, config.clientId)
      props.put(ConsumerConfig.GROUP_ID_CONFIG, config.groupId)

      // Consumer specific configurations
      props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, config.autoOffsetReset)
      props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, config.enableAutoCommit.toString)
      props.put(ConsumerConfig.AUTO_COMMIT_INTERVAL_MS_CONFIG, config.autoCommitIntervalMs.toString)
      props.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, config.sessionTimeoutMs.toString)
      props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, config.maxPollRecords.toString)
      props.put(ConsumerConfig.FETCH_MIN_BYTES_CONFIG, config.fetchMinBytes.toString)
      props.put(ConsumerConfig.FETCH_MAX_WAIT_MS_CONFIG, config.fetchMaxWaitMs.toString)
      props.put(ConsumerConfig.MAX_PARTITION_FETCH_BYTES_CONFIG, config.maxPartitionFetchBytes.toString)

      // Deserializers
      props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, classOf[StringDeserializer].getName)
      props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, classOf[StringDeserializer].getName)

      val consumer = new JKafkaConsumer[String, String](props)
      consumer.subscribe(java.util.Arrays.asList(config.topic))

      // Log for debugging
      F.delay(println(s"Kafka consumer created with properties: ${props.toString}"))

      consumer
    }

    Resource.make(createConsumer)(consumer => F.delay {
      println("Closing Kafka consumer...")
      consumer.close()
    }).map { consumer =>
      new EventConsumer[F] {
        override def consume: F[Unit] = {
          def poll: F[Unit] = for {
            _ <- cs.shift
            records <- F.delay(consumer.poll(java.time.Duration.ofMillis(100)))
            _ <- records.asScala.toList.traverse_ { record =>
              F.delay {
                println(s"""
                           |Received Kafka message:
                           |Topic: ${record.topic()}
                           |Partition: ${record.partition()}
                           |Offset: ${record.offset()}
                           |Key: ${record.key()}
                           |Value: ${record.value()}
                           |Timestamp: ${record.timestamp()}
                           |""".stripMargin)
              }
            }
            _ <- timer.sleep(100.milliseconds)
          } yield ()

          poll
        }
      }
    }
  }
}