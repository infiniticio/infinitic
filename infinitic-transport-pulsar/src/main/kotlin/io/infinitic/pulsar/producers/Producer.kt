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
package io.infinitic.pulsar.producers

import io.github.oshai.kotlinlogging.KotlinLogging
import io.infinitic.common.data.MillisDuration
import io.infinitic.common.messages.Envelope
import io.infinitic.common.messages.Message
import io.infinitic.pulsar.namer.Namer
import io.infinitic.pulsar.schemas.schemaDefinition
import org.apache.pulsar.client.api.BatcherBuilder
import org.apache.pulsar.client.api.Producer
import org.apache.pulsar.client.api.ProducerAccessMode
import org.apache.pulsar.client.api.PulsarClient
import org.apache.pulsar.client.api.Schema
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

class Producer(
  override val pulsarClient: PulsarClient,
  val producerConfig: ProducerConfig
) : Namer(pulsarClient) {

  val logger = KotlinLogging.logger {}

  inline fun <T : Message, reified S : Envelope<T>> sendAsync(
    message: T,
    after: MillisDuration,
    topic: String,
    producerName: String,
    key: String? = null
  ): CompletableFuture<Unit> {
    val producer = getProducer<T, S>(topic, producerName, key)

    logger.debug { "Sending to topic '$topic' after $after with key '$key' and message='$message'" }

    return producer
        .newMessage()
        .value(message.envelope())
        .also {
          if (key != null) {
            it.key(key)
          }
          if (after > 0) {
            it.deliverAfter(after.long, TimeUnit.MILLISECONDS)
          }
        }
        .sendAsync()
        // removing MessageId from the returned CompletableFuture
        .thenApplyAsync { }
  }

  @Suppress("UNCHECKED_CAST")
  inline fun <T : Message, reified S : Envelope<out T>> getProducer(
    topic: String,
    producerName: String,
    key: String?
  ) = producers.computeIfAbsent(topic) {
    logger.debug { "Creating Producer on topic '$topic' with name '$producerName' and key='$key'" }

    val schema = Schema.AVRO(schemaDefinition<S>())

    pulsarClient
        .newProducer(schema)
        .topic(topic)
        .producerName(producerName)
        .accessMode(ProducerAccessMode.Shared)
        .also { p ->
          key?.let { p.batcherBuilder(BatcherBuilder.KEY_BASED) }
          producerConfig.autoUpdatePartitions?.also {
            logger.info { "producer $producerName: autoUpdatePartitions=$it" }
            p.autoUpdatePartitions(it)
          }
          producerConfig.autoUpdatePartitionsIntervalSeconds?.also {
            logger.info { "producer $producerName: autoUpdatePartitionsInterval=$it" }
            p.autoUpdatePartitionsInterval((it * 1000).toInt(), TimeUnit.MILLISECONDS)
          }
          producerConfig.batchingMaxBytes?.also {
            logger.info { "producer $producerName: batchingMaxBytes=$it" }
            p.batchingMaxBytes(it)
          }
          producerConfig.batchingMaxMessages?.also {
            logger.info { "producer $producerName: batchingMaxMessages=$it" }
            p.batchingMaxMessages(it)
          }
          producerConfig.batchingMaxPublishDelaySeconds?.also {
            logger.info { "producer $producerName: batchingMaxPublishDelay=$it" }
            p.batchingMaxPublishDelay((it * 1000).toLong(), TimeUnit.MILLISECONDS)
          }
          producerConfig.compressionType?.also {
            logger.info { "producer $producerName: compressionType=$it" }
            p.compressionType(it)
          }
          producerConfig.cryptoFailureAction?.also {
            logger.info { "producer $producerName: cryptoFailureAction=$it" }
            p.cryptoFailureAction(it)
          }
          producerConfig.defaultCryptoKeyReader?.also {
            logger.info { "producer $producerName: defaultCryptoKeyReader=$it" }
            p.defaultCryptoKeyReader(it)
          }
          producerConfig.encryptionKey?.also {
            logger.info { "producer $producerName: addEncryptionKey=$it" }
            p.addEncryptionKey(it)
          }
          producerConfig.enableBatching?.also {
            logger.info { "producer $producerName: enableBatching=$it" }
            p.enableBatching(it)
          }
          producerConfig.enableChunking?.also {
            logger.info { "producer $producerName: enableChunking=$it" }
            p.enableChunking(it)
          }
          producerConfig.enableLazyStartPartitionedProducers?.also {
            logger.info { "producer $producerName: enableLazyStartPartitionedProducers=$it" }
            p.enableLazyStartPartitionedProducers(it)
          }
          producerConfig.enableMultiSchema?.also {
            logger.info { "producer $producerName: enableMultiSchema=$it" }
            p.enableMultiSchema(it)
          }
          producerConfig.hashingScheme?.also {
            logger.info { "producer $producerName: hashingScheme=$it" }
            p.hashingScheme(it)
          }
          producerConfig.messageRoutingMode?.also {
            logger.info { "producer $producerName: messageRoutingMode=$it" }
            p.messageRoutingMode(it)
          }
          producerConfig.properties?.also {
            logger.info { "producer $producerName: properties=$it" }
            p.properties(it)
          }
          producerConfig.roundRobinRouterBatchingPartitionSwitchFrequency?.also {
            logger.info {
              "producer $producerName: roundRobinRouterBatchingPartitionSwitchFrequency=$it"
            }
            p.roundRobinRouterBatchingPartitionSwitchFrequency(it)
          }
          producerConfig.sendTimeoutSeconds?.also {
            logger.info { "producer $producerName: sendTimeout=$it" }
            p.sendTimeout((it * 1000).toInt(), TimeUnit.MILLISECONDS)
          }
        }
        .blockIfQueueFull(producerConfig.blockIfQueueFull)
        .also {
          logger.info { "producer $producerName: blockIfQueueFull=${producerConfig.blockIfQueueFull}" }
        }
        .create()
  } as Producer<Envelope<out Message>>

  companion object {
    val producers = ConcurrentHashMap<String, Producer<out Envelope<out Message>>>()
  }
}
