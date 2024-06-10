/**
 * "Commons Clause" License Condition v1.0
 *
 * The Software is provided to you by the Licensor under the License, as defined below, subject to
 * the following condition.
 *
 * Without limiting other conditions in the License, the grant of rights under the License will not
 * include, and the License does not grant to you, the right to Sell the Software.
 *
 * For purposes of the foregoing, “Sell” means practicing any or all of the rights granted to you
 * under the License to provide to third parties, for a fee or other consideration (including
 * without limitation fees for hosting or consulting/ support services related to the Software), a
 * product or service whose value derives, entirely or substantially, from the functionality of the
 * Software. Any license notice or attribution required by the License must also include this
 * Commons Clause License Condition notice.
 *
 * Software: Infinitic
 *
 * License: MIT License (https://opensource.org/licenses/MIT)
 *
 * Licensor: infinitic.io
 */
package io.infinitic.pulsar.client

import io.github.oshai.kotlinlogging.KotlinLogging
import io.infinitic.common.messages.Envelope
import io.infinitic.common.messages.Message
import io.infinitic.pulsar.consumers.ConsumerConfig
import io.infinitic.pulsar.producers.ProducerConfig
import io.infinitic.pulsar.schemas.schemaDefinition
import kotlinx.coroutines.future.await
import org.apache.pulsar.client.api.BatcherBuilder
import org.apache.pulsar.client.api.Consumer
import org.apache.pulsar.client.api.DeadLetterPolicy
import org.apache.pulsar.client.api.Producer
import org.apache.pulsar.client.api.PulsarClient
import org.apache.pulsar.client.api.PulsarClientException
import org.apache.pulsar.client.api.Schema
import org.apache.pulsar.client.api.SubscriptionInitialPosition
import org.apache.pulsar.client.api.SubscriptionType
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import kotlin.reflect.KClass


class PulsarInfiniticClient(private val pulsarClient: PulsarClient) {

  private val logger = KotlinLogging.logger {}

  private lateinit var name: String

  /**
   * Useful to check the uniqueness of a connected producer's name or to provide a unique name
   */
  suspend fun getUniqueName(namerTopic: String, proposedName: String?): Result<String> {
    if (::name.isInitialized) return Result.success(name)

    // this consumer must stay active until client is closed
    // to prevent other clients to use the same name
    name = try {
      pulsarClient
          .newProducer()
          .topic(namerTopic)
          .also {
            proposedName?.let { name -> it.producerName(name) }
          }
          .createAsync()
          .await()
          .producerName
    } catch (e: PulsarClientException) {
      // if the producer name is already taken
      // the exception will be PulsarClientException.ProducerBusyException
      return Result.failure(e)
    }

    return Result.success(name)
  }

  /**
   * Closes a consumer and removes it from the list of consumers.
   *
   * @param consumer The consumer to close.
   * @return Result of the close operation.
   *         - Result.success(Unit) if the consumer was closed successfully.
   *         - Result.failure(e) if an error occurred during the close operation.
   */
  fun closeConsumer(consumer: Consumer<*>): Result<Unit> = try {
    consumer.close()
    Result.success(Unit)
  } catch (e: PulsarClientException) {
    Result.failure(e)
  }

  /**
   * Get existing producer or create a new one
   *
   * Returns:
   * - Result.success(Producer)
   * - Result.failure(e) in case of error
   */
  fun getProducer(
    topic: String,
    schemaClass: KClass<out Envelope<out Message>>,
    producerName: String,
    producerConfig: ProducerConfig,
    key: String? = null,
  ): Result<Producer<Envelope<out Message>>> {
    // get producer if it already exists
    return try {
      Result.success(
          producers.computeIfAbsent(topic) {
            createProducer(topic, schemaClass, producerName, producerConfig, key)
          },
      )
    } catch (e: PulsarClientException) {
      logger.warn(e) { "Unable to create producer $producerName on topic $topic" }
      Result.failure(e)
    }
  }

  private fun createProducer(
    topic: String,
    schemaClass: KClass<out Envelope<out Message>>,
    producerName: String,
    producerConfig: ProducerConfig,
    key: String? = null,
  ): Producer<Envelope<out Message>> {
    // otherwise create it
    logger.info { "Creating Producer '$producerName' on topic '$topic' ${key?.let { "with key='$key'" } ?: "without key"}" }

    val schema = Schema.AVRO(schemaDefinition(schemaClass))

    val builder = pulsarClient
        .newProducer(schema)
        .topic(topic)
        .producerName(producerName)

    with(builder) {
      key?.also { batcherBuilder(BatcherBuilder.KEY_BASED) }

      producerConfig.autoUpdatePartitions?.also {
        logger.info { "Producer $producerName: autoUpdatePartitions=$it" }
        autoUpdatePartitions(it)
      }
      producerConfig.autoUpdatePartitionsIntervalSeconds?.also {
        logger.info { "Producer $producerName: autoUpdatePartitionsInterval=$it" }
        autoUpdatePartitionsInterval((it * 1000).toInt(), TimeUnit.MILLISECONDS)
      }
      producerConfig.batchingMaxBytes?.also {
        logger.info { "Producer $producerName: batchingMaxBytes=$it" }
        batchingMaxBytes(it)
      }
      producerConfig.batchingMaxMessages?.also {
        logger.info { "Producer $producerName: batchingMaxMessages=$it" }
        batchingMaxMessages(it)
      }
      producerConfig.batchingMaxPublishDelaySeconds?.also {
        logger.info { "Producer $producerName: batchingMaxPublishDelay=$it" }
        batchingMaxPublishDelay((it * 1000).toLong(), TimeUnit.MILLISECONDS)
      }
      producerConfig.compressionType?.also {
        logger.info { "Producer $producerName: compressionType=$it" }
        compressionType(it)
      }
      producerConfig.cryptoFailureAction?.also {
        logger.info { "Producer $producerName: cryptoFailureAction=$it" }
        cryptoFailureAction(it)
      }
      producerConfig.defaultCryptoKeyReader?.also {
        logger.info { "Producer $producerName: defaultCryptoKeyReader=$it" }
        defaultCryptoKeyReader(it)
      }
      producerConfig.encryptionKey?.also {
        logger.info { "Producer $producerName: addEncryptionKey=$it" }
        addEncryptionKey(it)
      }
      producerConfig.enableBatching?.also {
        logger.info { "Producer $producerName: enableBatching=$it" }
        enableBatching(it)
      }
      producerConfig.enableChunking?.also {
        logger.info { "Producer $producerName: enableChunking=$it" }
        enableChunking(it)
      }
      producerConfig.enableLazyStartPartitionedProducers?.also {
        logger.info { "Producer $producerName: enableLazyStartPartitionedProducers=$it" }
        enableLazyStartPartitionedProducers(it)
      }
      producerConfig.enableMultiSchema?.also {
        logger.info { "Producer $producerName: enableMultiSchema=$it" }
        enableMultiSchema(it)
      }
      producerConfig.hashingScheme?.also {
        logger.info { "Producer $producerName: hashingScheme=$it" }
        hashingScheme(it)
      }
      producerConfig.messageRoutingMode?.also {
        logger.info { "Producer $producerName: messageRoutingMode=$it" }
        messageRoutingMode(it)
      }
      producerConfig.properties?.also {
        logger.info { "Producer $producerName: properties=$it" }
        properties(it)
      }
      producerConfig.roundRobinRouterBatchingPartitionSwitchFrequency?.also {
        logger.info { "Producer $producerName: roundRobinRouterBatchingPartitionSwitchFrequency=$it" }
        roundRobinRouterBatchingPartitionSwitchFrequency(it)
      }
      producerConfig.sendTimeoutSeconds?.also {
        logger.info { "Producer $producerName: sendTimeout=$it" }
        sendTimeout((it * 1000).toInt(), TimeUnit.MILLISECONDS)
      }
      blockIfQueueFull(producerConfig.blockIfQueueFull).also {
        logger.info { "Producer $producerName: blockIfQueueFull=${producerConfig.blockIfQueueFull}" }
      }
    }

    @Suppress("UNCHECKED_CAST")
    return builder.create() as Producer<Envelope<out Message>>
  }

  /** Create a new consumer
   *
   * Returns:
   * - Result.success(Consumer)
   * - Result.failure(e) in case of error
   */
  internal fun <S : Envelope<out Message>> newConsumer(
    schema: Schema<S>,
    consumerDef: ConsumerDef,
    consumerDefDlq: ConsumerDef? = null,
  ): Result<Consumer<S>> {

    val (topic,
        subscriptionName,
        subscriptionType,
        subscriptionInitialPosition,
        consumerName,
        consumerConfig
    ) = consumerDef

    val builder = pulsarClient
        .newConsumer(schema)
        .topic(topic)
        .subscriptionType(subscriptionType)
        .subscriptionName(subscriptionName)
        .consumerName(consumerName)
        .subscriptionInitialPosition(subscriptionInitialPosition)

    // Dead Letter Queue
    consumerDefDlq?.let {
      builder
          .deadLetterPolicy(
              DeadLetterPolicy
                  .builder()
                  .maxRedeliverCount(consumerConfig.getMaxRedeliverCount())
                  .deadLetterTopic(it.topic)
                  .build(),
          )
          // remove default ackTimeout set by the deadLetterPolicy
          // see https://github.com/apache/pulsar/issues/8484
          .ackTimeout(0, TimeUnit.MILLISECONDS)

      // to avoid immediate deletion of messages in DLQ, we immediately create a subscription
      val consumerDlq =
          newConsumer(schema, it).getOrElse { throwable ->
            logger.error { "Unable to create consumer on DLQ topic ${it.topic}" }
            return Result.failure(throwable)
          }
      try {
        // we close the consumer immediately as we do not need it
        consumerDlq.close()
      } catch (e: PulsarClientException) {
        logger.warn { "Unable to close consumer on DLQ topic ${it.topic}" }
        return Result.failure(e)
      }
    }

    with(builder) {
      // must be set AFTER deadLetterPolicy
      // see https://github.com/apache/pulsar/issues/8484
      consumerConfig.ackTimeoutSeconds?.also {
        logger.info {
          "subscription $subscriptionName: ackTimeout=${consumerConfig.ackTimeoutSeconds}"
        }
        ackTimeout(
            (consumerConfig.ackTimeoutSeconds * 1000).toLong(),
            TimeUnit.MILLISECONDS,
        )
      }
      consumerConfig.loadConf?.also {
        logger.info { "subscription $subscriptionName: loadConf=$it" }
        loadConf(it)
      }
      consumerConfig.subscriptionProperties?.also {
        logger.info { "subscription $subscriptionName: subscriptionProperties=$it" }
        subscriptionProperties(it)
      }
      consumerConfig.isAckReceiptEnabled?.also {
        logger.info { "subscription $subscriptionName: isAckReceiptEnabled=$it" }
        isAckReceiptEnabled(it)
      }
      consumerConfig.ackTimeoutTickTimeSeconds?.also {
        logger.info { "subscription $subscriptionName: ackTimeoutTickTime=$it" }
        ackTimeoutTickTime((it * 1000).toLong(), TimeUnit.MILLISECONDS)
      }
      consumerConfig.negativeAckRedeliveryDelaySeconds?.also {
        logger.info { "subscription $subscriptionName: negativeAckRedeliveryDelay=$it" }
        negativeAckRedeliveryDelay((it * 1000).toLong(), TimeUnit.MILLISECONDS)
      }
      consumerConfig.defaultCryptoKeyReader?.also {
        logger.info { "subscription $subscriptionName: defaultCryptoKeyReader=$it" }
        defaultCryptoKeyReader(it)
      }
      consumerConfig.cryptoFailureAction?.also {
        logger.info { "subscription $subscriptionName: cryptoFailureAction=$it" }
        cryptoFailureAction(it)
      }
      consumerConfig.receiverQueueSize?.also {
        logger.info { "subscription $subscriptionName: receiverQueueSize=$it" }
        receiverQueueSize(it)
      }
      consumerConfig.acknowledgmentGroupTimeSeconds?.also {
        logger.info { "subscription $subscriptionName: acknowledgmentGroupTime=$it" }
        acknowledgmentGroupTime((it * 1000).toLong(), TimeUnit.MILLISECONDS)
      }
      consumerConfig.replicateSubscriptionState?.also {
        logger.info { "subscription $subscriptionName: replicateSubscriptionState=$it" }
        replicateSubscriptionState(it)
      }
      consumerConfig.maxTotalReceiverQueueSizeAcrossPartitions?.also {
        logger.info {
          "subscription $subscriptionName: maxTotalReceiverQueueSizeAcrossPartitions=$it"
        }
        maxTotalReceiverQueueSizeAcrossPartitions(it)
      }
      consumerConfig.priorityLevel?.also {
        logger.info { "subscription $subscriptionName: priorityLevel=$it" }
        priorityLevel(it)
      }
      consumerConfig.properties?.also {
        logger.info { "subscription $subscriptionName: properties=$it" }
        properties(it)
      }
      consumerConfig.autoUpdatePartitions?.also {
        logger.info { "subscription $subscriptionName: autoUpdatePartitions=$it" }
        autoUpdatePartitions(it)
      }
      consumerConfig.autoUpdatePartitionsIntervalSeconds?.also {
        logger.info { "subscription $subscriptionName: autoUpdatePartitionsInterval=$it" }
        autoUpdatePartitionsInterval((it * 1000).toInt(), TimeUnit.MILLISECONDS)
      }
      consumerConfig.enableBatchIndexAcknowledgment?.also {
        logger.info { "subscription $subscriptionName: enableBatchIndexAcknowledgment=$it" }
        enableBatchIndexAcknowledgment(it)
      }
      consumerConfig.maxPendingChunkedMessage?.also {
        logger.info { "subscription $subscriptionName: maxPendingChunkedMessage=$it" }
        maxPendingChunkedMessage(it)
      }
      consumerConfig.autoAckOldestChunkedMessageOnQueueFull?.also {
        logger.info {
          "subscription $subscriptionName: autoAckOldestChunkedMessageOnQueueFull=$it"
        }
        autoAckOldestChunkedMessageOnQueueFull(it)
      }
      consumerConfig.expireTimeOfIncompleteChunkedMessageSeconds?.also {
        logger.info {
          "subscription $subscriptionName: expireTimeOfIncompleteChunkedMessage=$it"
        }
        expireTimeOfIncompleteChunkedMessage((it * 1000).toLong(), TimeUnit.MILLISECONDS)
      }
      consumerConfig.startPaused?.also {
        logger.info { "subscription $subscriptionName: startPaused=$it" }
        startPaused(it)
      }
    }

    return try {
      val consumer = builder.subscribe()
      Result.success(consumer)
    } catch (e: PulsarClientException) {
      logger.error(e) { "Unable to create consumer $consumerName on topic $topic" }
      Result.failure(e)
    }
  }

  // Convenience class to create a consumer
  internal data class ConsumerDef(
    val topic: String,
    val subscriptionName: String,
    val subscriptionType: SubscriptionType,
    val subscriptionInitialPosition: SubscriptionInitialPosition,
    val consumerName: String,
    val consumerConfig: ConsumerConfig,
  )

  companion object {
    // producer per topic
    val producers = ConcurrentHashMap<String, Producer<Envelope<out Message>>>()
  }
}
