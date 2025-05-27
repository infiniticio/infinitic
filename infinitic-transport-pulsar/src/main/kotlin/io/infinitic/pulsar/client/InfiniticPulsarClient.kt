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
import io.infinitic.common.transport.config.BatchConfig
import io.infinitic.common.transport.config.maxMillis
import io.infinitic.pulsar.config.PulsarConsumerConfig
import io.infinitic.pulsar.config.PulsarProducerConfig
import io.infinitic.pulsar.schemas.schemaDefinition
import kotlinx.coroutines.future.await
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.apache.pulsar.client.api.BatchReceivePolicy
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


class InfiniticPulsarClient(private val pulsarClient: PulsarClient) {

  private val logger = KotlinLogging.logger {}

  lateinit var name: String

  private val nameMutex = Mutex()

  /**
   * Check the uniqueness of a connected producer's name or to provide a unique name
   */
  suspend fun initName(namerTopic: String, suggestedName: String?): Result<String> {
    if (::name.isInitialized) return Result.success(name)

    return nameMutex.withLock {
      if (::name.isInitialized) return Result.success(name)

      // this consumer must stay active until client is closed
      // to prevent other clients to use the same name
      val producer = try {
        pulsarClient
            .newProducer()
            .topic(namerTopic)
            .also {
              suggestedName?.let { name -> it.producerName(name) }
            }
            .createAsync()
            .await()
      } catch (e: PulsarClientException.ProducerBusyException) {
        logger.error(e) { "Name $suggestedName already in use" }
        return Result.failure(e)
      } catch (e: PulsarClientException) {
        return Result.failure(e)
      }
      name = producer.producerName

      // close producer
      try {
        producer.closeAsync().await()
      } catch (e: PulsarClientException) {
        logWarn(e) { "Unable to close producer on topic $namerTopic" }
      }

      Result.success(name)
    }
  }

  /**
   * Get existing producer or create a new one
   *
   * Returns:
   * - Result.success(Producer)
   * - Result.failure(e) in case of error
   */
  fun <T : Message> getProducer(
    topic: String,
    schemaKClass: KClass<out Envelope<out T>>,
    batchSendingConfig: BatchConfig?,
    pulsarProducerConfig: PulsarProducerConfig,
    key: String? = null,
  ): Result<Producer<Envelope<out T>>> {
    // get producer if it already exists
    return try {
      @Suppress("UNCHECKED_CAST")
      Result.success(
          producers.computeIfAbsent(topic) {
            createProducer(topic, schemaKClass, pulsarProducerConfig, batchSendingConfig, key)
          } as Producer<Envelope<out T>>,
      )
    } catch (e: PulsarClientException) {
      logWarn(e) { "Unable to create producer on topic $topic" }
      Result.failure(e)
    }
  }

  internal fun createProducer(
    topic: String,
    schemaKClass: KClass<out Envelope<out Message>>,
    pulsarProducerConfig: PulsarProducerConfig,
    batchSendingConfig: BatchConfig?,
    key: String? = null,
  ): Producer<Envelope<out Message>> {
    // otherwise create it
    logInfo { "Creating Producer on topic '$topic' ${key?.let { "with key='$key'" } ?: "without key"}" }

    val schema = Schema.AVRO(schemaDefinition(schemaKClass))

    val builder = pulsarClient
        .newProducer(schema)
        .topic(topic)
        .producerName(name)

    with(builder) {
      key?.also { batcherBuilder(BatcherBuilder.KEY_BASED) }

      pulsarProducerConfig.autoUpdatePartitions?.also {
        logInfo { "Producer autoUpdatePartitions=$it" }
        autoUpdatePartitions(it)
      }
      pulsarProducerConfig.autoUpdatePartitionsIntervalSeconds?.also {
        logInfo { "Producer autoUpdatePartitionsInterval=$it" }
        autoUpdatePartitionsInterval((it * 1000).toInt(), TimeUnit.MILLISECONDS)
      }
      pulsarProducerConfig.batchingMaxBytes?.also {
        logInfo { "Producer batchingMaxBytes=$it" }
        batchingMaxBytes(it)
      }
      pulsarProducerConfig.batchingMaxMessages?.also {
        logInfo { "Producer batchingMaxMessages=$it" }
        batchingMaxMessages(it)
      }
      pulsarProducerConfig.batchingMaxPublishDelaySeconds?.also {
        logInfo { "Producer batchingMaxPublishDelay=$it" }
        batchingMaxPublishDelay((it * 1000).toLong(), TimeUnit.MILLISECONDS)
      }
      pulsarProducerConfig.compressionType?.also {
        logInfo { "Producer compressionType=$it" }
        compressionType(it)
      }
      pulsarProducerConfig.cryptoFailureAction?.also {
        logInfo { "Producer cryptoFailureAction=$it" }
        cryptoFailureAction(it)
      }
      pulsarProducerConfig.defaultCryptoKeyReader?.also {
        logInfo { "Producer defaultCryptoKeyReader=$it" }
        defaultCryptoKeyReader(it)
      }
      pulsarProducerConfig.encryptionKey?.also {
        logInfo { "Producer addEncryptionKey=$it" }
        addEncryptionKey(it)
      }
      pulsarProducerConfig.enableBatching?.also {
        logInfo { "Producer enableBatching=$it" }
        enableBatching(it)
      }
      pulsarProducerConfig.enableChunking?.also {
        logInfo { "Producer enableChunking=$it" }
        enableChunking(it)
      }
      pulsarProducerConfig.enableLazyStartPartitionedProducers?.also {
        logInfo { "Producer enableLazyStartPartitionedProducers=$it" }
        enableLazyStartPartitionedProducers(it)
      }
      pulsarProducerConfig.enableMultiSchema?.also {
        logInfo { "Producer enableMultiSchema=$it" }
        enableMultiSchema(it)
      }
      pulsarProducerConfig.hashingScheme?.also {
        logInfo { "Producer hashingScheme=$it" }
        hashingScheme(it)
      }
      pulsarProducerConfig.messageRoutingMode?.also {
        logInfo { "Producer messageRoutingMode=$it" }
        messageRoutingMode(it)
      }
      pulsarProducerConfig.properties?.also {
        logInfo { "Producer properties=$it" }
        properties(it)
      }
      pulsarProducerConfig.roundRobinRouterBatchingPartitionSwitchFrequency?.also {
        logInfo { "Producer roundRobinRouterBatchingPartitionSwitchFrequency=$it" }
        roundRobinRouterBatchingPartitionSwitchFrequency(it)
      }
      pulsarProducerConfig.sendTimeoutSeconds?.also {
        logInfo { "Producer sendTimeout=$it" }
        sendTimeout((it * 1000).toInt(), TimeUnit.MILLISECONDS)
      }
      blockIfQueueFull(pulsarProducerConfig.blockIfQueueFull).also {
        logInfo { "Producer blockIfQueueFull=${pulsarProducerConfig.blockIfQueueFull}" }
      }

      // if batchConfig is defined, it replaces above settings
      batchSendingConfig?.also {
        logInfo { "Producer batchConfig=$it" }
        batchingMaxMessages(it.maxMessages)
        batchingMaxPublishDelay(it.maxMillis, TimeUnit.MILLISECONDS)
        enableBatching(true)
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

    logInfo { "Creating consumer with $consumerDef" }

    val (topic,
        subscriptionName,
        subscriptionType,
        consumerName,
        batchReceivingConfig,
        consumerConfig
    ) = consumerDef

    val builder = pulsarClient
        .newConsumer(schema)
        .topic(topic)
        .subscriptionType(subscriptionType)
        .subscriptionName(subscriptionName)
        .consumerName(consumerName)
        .subscriptionInitialPosition(SubscriptionInitialPosition.Earliest)

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
      val consumerDlq = newConsumer(schema, it).getOrElse { throwable ->
        logError(throwable) { "Unable to create consumer on DLQ topic ${it.topic}" }
        return Result.failure(throwable)
      }
      try {
        // we close the consumer immediately as we do not need it
        consumerDlq.close()
      } catch (e: PulsarClientException) {
        logWarn(e) { "Unable to close consumer on DLQ topic ${it.topic}" }
        return Result.failure(e)
      }
    }

    with(builder) {
      // must be set AFTER deadLetterPolicy
      // see https://github.com/apache/pulsar/issues/8484
      consumerConfig.ackTimeoutSeconds?.also {
        logInfo { "Subscription $subscriptionName: ackTimeout=${consumerConfig.ackTimeoutSeconds}" }
        ackTimeout(
            (consumerConfig.ackTimeoutSeconds * 1000).toLong(),
            TimeUnit.MILLISECONDS,
        )
      }
      consumerConfig.loadConf?.also {
        logInfo { "Subscription $subscriptionName: loadConf=$it" }
        loadConf(it)
      }
      consumerConfig.subscriptionProperties?.also {
        logInfo { "Subscription $subscriptionName: subscriptionProperties=$it" }
        subscriptionProperties(it)
      }
      consumerConfig.isAckReceiptEnabled?.also {
        logInfo { "Subscription $subscriptionName: isAckReceiptEnabled=$it" }
        isAckReceiptEnabled(it)
      }
      consumerConfig.ackTimeoutTickTimeSeconds?.also {
        logInfo { "Subscription $subscriptionName: ackTimeoutTickTime=$it" }
        ackTimeoutTickTime((it * 1000).toLong(), TimeUnit.MILLISECONDS)
      }
      consumerConfig.negativeAckRedeliveryDelaySeconds?.also {
        logInfo { "Subscription $subscriptionName: negativeAckRedeliveryDelay=$it" }
        negativeAckRedeliveryDelay((it * 1000).toLong(), TimeUnit.MILLISECONDS)
      }
      consumerConfig.defaultCryptoKeyReader?.also {
        logInfo { "Subscription $subscriptionName: defaultCryptoKeyReader=$it" }
        defaultCryptoKeyReader(it)
      }
      consumerConfig.cryptoFailureAction?.also {
        logInfo { "Subscription $subscriptionName: cryptoFailureAction=$it" }
        cryptoFailureAction(it)
      }
      consumerConfig.receiverQueueSize?.also {
        logInfo { "Subscription $subscriptionName: receiverQueueSize=$it" }
        receiverQueueSize(it)
      }
      consumerConfig.acknowledgmentGroupTimeSeconds?.also {
        logInfo { "Subscription $subscriptionName: acknowledgmentGroupTime=$it" }
        acknowledgmentGroupTime((it * 1000).toLong(), TimeUnit.MILLISECONDS)
      }
      consumerConfig.replicateSubscriptionState?.also {
        logInfo { "Subscription $subscriptionName: replicateSubscriptionState=$it" }
        replicateSubscriptionState(it)
      }
      consumerConfig.maxTotalReceiverQueueSizeAcrossPartitions?.also {
        logInfo { "Subscription $subscriptionName: maxTotalReceiverQueueSizeAcrossPartitions=$it" }
        maxTotalReceiverQueueSizeAcrossPartitions(it)
      }
      consumerConfig.priorityLevel?.also {
        logInfo { "Subscription $subscriptionName: priorityLevel=$it" }
        priorityLevel(it)
      }
      consumerConfig.properties?.also {
        logInfo { "Subscription $subscriptionName: properties=$it" }
        properties(it)
      }
      consumerConfig.autoUpdatePartitions?.also {
        logInfo { "Subscription $subscriptionName: autoUpdatePartitions=$it" }
        autoUpdatePartitions(it)
      }
      consumerConfig.autoUpdatePartitionsIntervalSeconds?.also {
        logInfo { "Subscription $subscriptionName: autoUpdatePartitionsInterval=$it" }
        autoUpdatePartitionsInterval((it * 1000).toInt(), TimeUnit.MILLISECONDS)
      }
      consumerConfig.enableBatchIndexAcknowledgment?.also {
        logInfo { "Subscription $subscriptionName: enableBatchIndexAcknowledgment=$it" }
        enableBatchIndexAcknowledgment(it)
      }
      consumerConfig.maxPendingChunkedMessage?.also {
        logInfo { "Subscription $subscriptionName: maxPendingChunkedMessage=$it" }
        maxPendingChunkedMessage(it)
      }
      consumerConfig.autoAckOldestChunkedMessageOnQueueFull?.also {
        logInfo { "Subscription $subscriptionName: autoAckOldestChunkedMessageOnQueueFull=$it" }
        autoAckOldestChunkedMessageOnQueueFull(it)
      }
      consumerConfig.expireTimeOfIncompleteChunkedMessageSeconds?.also {
        logInfo { "Subscription $subscriptionName: expireTimeOfIncompleteChunkedMessage=$it" }
        expireTimeOfIncompleteChunkedMessage((it * 1000).toLong(), TimeUnit.MILLISECONDS)
      }
      consumerConfig.startPaused?.also {
        logInfo { "Subscription $subscriptionName: startPaused=$it" }
        startPaused(it)
      }

      // Batch Receive Policy
      batchReceivingConfig?.also {
        logInfo { "Subscription $subscriptionName: batchConfig=$it" }
        batchReceivePolicy(
            BatchReceivePolicy.builder()
                .maxNumMessages(it.maxMessages)
                .timeout((it.maxSeconds * 1000).toInt(), TimeUnit.MILLISECONDS)
                .build(),
        )
      }
    }

    return try {
      Result.success(builder.subscribe())
    } catch (e: PulsarClientException) {
      logError(e) { "Unable to create consumer $consumerName on topic $topic" }
      Result.failure(e)
    }
  }

  // Convenience class to create a consumer
  internal data class ConsumerDef(
    val topic: String,
    val subscriptionName: String,
    val subscriptionType: SubscriptionType,
    val consumerName: String,
    val batchReceivingConfig: BatchConfig?,
    val pulsarConsumerConfig: PulsarConsumerConfig,
  )

  private fun logWarn(e: Exception, txt: () -> String) {
    logger.warn(e) { "Client $name: ${txt()}" }
  }

  private fun logError(e: Throwable, txt: () -> String) {
    logger.error(e) { "Client $name: ${txt()}" }
  }

  private fun logInfo(txt: () -> String) {
    logger.info { "Client $name: ${txt()}" }
  }

  companion object {
    // producer per topic
    val producers = ConcurrentHashMap<String, Producer<Envelope<out Message>>>()

    @JvmStatic
    fun clearCaches() {
      producers.clear()
    }
  }
}
