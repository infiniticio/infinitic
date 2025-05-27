/**
 * "Commons Clause" License Condition v1.0
 *
 * The Software is provided to you by the Licensor under the License, as defined below, subject to
 * the following condition.
 *
 * Without limiting other conditions in the License, the grant of rights under the License will not
 * include, and the License does not grant to you, the right to Sell the Software.
 *
 * For purposes of the foregoing, "Sell" means practicing any or all of the rights granted to you
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

import io.infinitic.common.fixtures.TestFactory
import io.infinitic.common.messages.Envelope
import io.infinitic.common.messages.Message
import io.infinitic.common.transport.config.BatchConfig
import io.infinitic.common.transport.config.maxMillis
import io.infinitic.common.workflows.engine.messages.WorkflowEngineEnvelope
import io.infinitic.pulsar.config.PulsarProducerConfig
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit
import org.apache.pulsar.client.api.BatcherBuilder
import org.apache.pulsar.client.api.CompressionType
import org.apache.pulsar.client.api.HashingScheme
import org.apache.pulsar.client.api.MessageRoutingMode
import org.apache.pulsar.client.api.Producer
import org.apache.pulsar.client.api.ProducerBuilder
import org.apache.pulsar.client.api.ProducerCryptoFailureAction
import org.apache.pulsar.client.api.PulsarClient
import org.apache.pulsar.client.api.Schema

class PulsarInfiniticProducerTests : StringSpec(
    {
      // producer properties slots
      val topic = slot<String>()
      val producerName = slot<String>()
      val batcherBuilder = slot<BatcherBuilder>()
      val autoUpdatePartitions = slot<Boolean>()
      val autoUpdatePartitionsInterval = slot<Int>()
      val autoUpdatePartitionsIntervalUnit = slot<TimeUnit>()
      val batchingMaxBytes = slot<Int>()
      val batchingMaxMessages = slot<Int>()
      val batchingMaxPublishDelay = slot<Long>()
      val batchingMaxPublishDelayUnit = slot<TimeUnit>()
      val compressionType = slot<CompressionType>()
      val cryptoFailureAction = slot<ProducerCryptoFailureAction>()
      val encryptionKey = slot<String>()
      val enableBatching = slot<Boolean>()
      val enableChunking = slot<Boolean>()
      val enableLazyStartPartitionedProducers = slot<Boolean>()
      val enableMultiSchema = slot<Boolean>()
      val hashingScheme = slot<HashingScheme>()
      val messageRoutingMode = slot<MessageRoutingMode>()
      val properties = slot<Map<String, String>>()
      val roundRobinRouterBatchingPartitionSwitchFrequency = slot<Int>()
      val sendTimeout = slot<Int>()
      val sendTimeoutUnit = slot<TimeUnit>()
      val blockIfQueueFull = slot<Boolean>()

      // mocks
      fun getProducer() = mockk<Producer<Envelope<out Message>>> {
        every { sendAsync(any()) } returns CompletableFuture.completedFuture(null)
        every { close() } returns Unit
      }

      fun getProducerBuilder() = mockk<ProducerBuilder<Envelope<out Message>>> {
        every { create() } returns getProducer()
        every { topic(capture(topic)) } returns this
        every { producerName(capture(producerName)) } returns this
        every { batcherBuilder(capture(batcherBuilder)) } returns this
        every { autoUpdatePartitions(capture(autoUpdatePartitions)) } returns this
        every {
          autoUpdatePartitionsInterval(
              capture(autoUpdatePartitionsInterval),
              capture(autoUpdatePartitionsIntervalUnit),
          )
        } returns this
        every { batchingMaxBytes(capture(batchingMaxBytes)) } returns this
        every { batchingMaxMessages(capture(batchingMaxMessages)) } returns this
        every {
          batchingMaxPublishDelay(
              capture(batchingMaxPublishDelay),
              capture(batchingMaxPublishDelayUnit),
          )
        } returns this
        every { compressionType(capture(compressionType)) } returns this
        every { cryptoFailureAction(capture(cryptoFailureAction)) } returns this
        every { addEncryptionKey(capture(encryptionKey)) } returns this
        every { enableBatching(capture(enableBatching)) } returns this
        every { enableChunking(capture(enableChunking)) } returns this
        every { enableLazyStartPartitionedProducers(capture(enableLazyStartPartitionedProducers)) } returns this
        every { enableMultiSchema(capture(enableMultiSchema)) } returns this
        every { hashingScheme(capture(hashingScheme)) } returns this
        every { messageRoutingMode(capture(messageRoutingMode)) } returns this
        every { properties(capture(properties)) } returns this
        every {
          roundRobinRouterBatchingPartitionSwitchFrequency(
              capture(
                  roundRobinRouterBatchingPartitionSwitchFrequency,
              ),
          )
        } returns this
        every { sendTimeout(capture(sendTimeout), capture(sendTimeoutUnit)) } returns this
        every { blockIfQueueFull(capture(blockIfQueueFull)) } returns this
      }

      val pulsarClient = mockk<PulsarClient> {
        every { newProducer(any<Schema<Envelope<out Message>>>()) } returns getProducerBuilder()
      }

      val client = InfiniticPulsarClient(pulsarClient).apply { name = "test" }

      "Configuration given should be applied to producer (no batch config)" {
        val randomConfig =
            TestFactory.random<PulsarProducerConfig>(mapOf("defaultCryptoKeyReader" to null))
        val randomTopic = TestFactory.random<String>()

        // when
        client.createProducer(
            topic = randomTopic,
            schemaKClass = WorkflowEngineEnvelope::class,
            pulsarProducerConfig = randomConfig,
            batchSendingConfig = null,
            key = null,
        )

        // then
        topic.captured shouldBe randomTopic
        producerName.captured shouldBe "test"

        batcherBuilder.isCaptured shouldBe false

        autoUpdatePartitions.captured shouldBe randomConfig.autoUpdatePartitions
        autoUpdatePartitionsInterval.captured shouldBe (randomConfig.autoUpdatePartitionsIntervalSeconds!! * 1000).toInt()
        autoUpdatePartitionsIntervalUnit.captured shouldBe TimeUnit.MILLISECONDS
        batchingMaxBytes.captured shouldBe randomConfig.batchingMaxBytes
        batchingMaxMessages.captured shouldBe randomConfig.batchingMaxMessages
        batchingMaxPublishDelay.captured shouldBe (randomConfig.batchingMaxPublishDelaySeconds!! * 1000).toLong()
        batchingMaxPublishDelayUnit.captured shouldBe TimeUnit.MILLISECONDS
        compressionType.captured shouldBe randomConfig.compressionType
        cryptoFailureAction.captured shouldBe randomConfig.cryptoFailureAction
        //defaultCryptoKeyReader.captured shouldBe randomConfig.defaultCryptoKeyReader
        encryptionKey.captured shouldBe randomConfig.encryptionKey
        enableBatching.captured shouldBe randomConfig.enableBatching
        enableChunking.captured shouldBe randomConfig.enableChunking
        enableLazyStartPartitionedProducers.captured shouldBe randomConfig.enableLazyStartPartitionedProducers
        enableMultiSchema.captured shouldBe randomConfig.enableMultiSchema
        hashingScheme.captured shouldBe randomConfig.hashingScheme
        messageRoutingMode.captured shouldBe randomConfig.messageRoutingMode
        properties.captured shouldBe randomConfig.properties
        roundRobinRouterBatchingPartitionSwitchFrequency.captured shouldBe randomConfig.roundRobinRouterBatchingPartitionSwitchFrequency
        sendTimeout.captured shouldBe (randomConfig.sendTimeoutSeconds!! * 1000).toInt()
        sendTimeoutUnit.captured shouldBe TimeUnit.MILLISECONDS
        blockIfQueueFull.captured shouldBe randomConfig.blockIfQueueFull
      }

      "Configuration given should be applied to producer (with batch config)" {
        val randomConfig =
            TestFactory.random<PulsarProducerConfig>(mapOf("defaultCryptoKeyReader" to null))
        val randomTopic = TestFactory.random<String>()
        val randomSchemaKClass = WorkflowEngineEnvelope::class
        val randomBatchConfig = TestFactory.random<BatchConfig>()

        // when
        client.createProducer(
            topic = randomTopic,
            schemaKClass = randomSchemaKClass,
            pulsarProducerConfig = randomConfig,
            batchSendingConfig = randomBatchConfig,
            key = null,
        )

        // then
        topic.captured shouldBe randomTopic
        producerName.captured shouldBe "test"

        batcherBuilder.isCaptured shouldBe false

        // Batch config should override batching settings
        batchingMaxMessages.captured shouldBe randomBatchConfig.maxMessages
        batchingMaxPublishDelay.captured shouldBe randomBatchConfig.maxMillis
        enableBatching.captured shouldBe true

        // Other settings should still be applied
        autoUpdatePartitions.captured shouldBe randomConfig.autoUpdatePartitions
        compressionType.captured shouldBe randomConfig.compressionType
        cryptoFailureAction.captured shouldBe randomConfig.cryptoFailureAction
        //defaultCryptoKeyReader.captured shouldBe randomConfig.defaultCryptoKeyReader
        encryptionKey.captured shouldBe randomConfig.encryptionKey
        enableChunking.captured shouldBe randomConfig.enableChunking
        enableLazyStartPartitionedProducers.captured shouldBe randomConfig.enableLazyStartPartitionedProducers
        enableMultiSchema.captured shouldBe randomConfig.enableMultiSchema
        hashingScheme.captured shouldBe randomConfig.hashingScheme
        messageRoutingMode.captured shouldBe randomConfig.messageRoutingMode
        properties.captured shouldBe randomConfig.properties
        roundRobinRouterBatchingPartitionSwitchFrequency.captured shouldBe randomConfig.roundRobinRouterBatchingPartitionSwitchFrequency
        sendTimeout.captured shouldBe (randomConfig.sendTimeoutSeconds!! * 1000).toInt()
        sendTimeoutUnit.captured shouldBe TimeUnit.MILLISECONDS
        blockIfQueueFull.captured shouldBe randomConfig.blockIfQueueFull
      }

      "Configuration given should be applied to producer (with key)" {
        val randomConfig =
            TestFactory.random<PulsarProducerConfig>(mapOf("defaultCryptoKeyReader" to null))
        val randomTopic = TestFactory.random<String>()
        val randomSchemaKClass = WorkflowEngineEnvelope::class
        val randomKey = TestFactory.random<String>()

        // when
        client.createProducer(
            topic = randomTopic,
            schemaKClass = randomSchemaKClass,
            pulsarProducerConfig = randomConfig,
            batchSendingConfig = null,
            key = randomKey,
        )

        // then
        topic.captured shouldBe randomTopic
        producerName.captured shouldBe "test"

        // Key-based batcher should be set
        batcherBuilder.captured shouldBe BatcherBuilder.KEY_BASED

        // Other settings should be applied
        autoUpdatePartitions.captured shouldBe randomConfig.autoUpdatePartitions
        compressionType.captured shouldBe randomConfig.compressionType
        cryptoFailureAction.captured shouldBe randomConfig.cryptoFailureAction
        //defaultCryptoKeyReader.captured shouldBe randomConfig.defaultCryptoKeyReader
        encryptionKey.captured shouldBe randomConfig.encryptionKey
        enableBatching.captured shouldBe randomConfig.enableBatching
        enableChunking.captured shouldBe randomConfig.enableChunking
        enableLazyStartPartitionedProducers.captured shouldBe randomConfig.enableLazyStartPartitionedProducers
        enableMultiSchema.captured shouldBe randomConfig.enableMultiSchema
        hashingScheme.captured shouldBe randomConfig.hashingScheme
        messageRoutingMode.captured shouldBe randomConfig.messageRoutingMode
        properties.captured shouldBe randomConfig.properties
        roundRobinRouterBatchingPartitionSwitchFrequency.captured shouldBe randomConfig.roundRobinRouterBatchingPartitionSwitchFrequency
        sendTimeout.captured shouldBe (randomConfig.sendTimeoutSeconds!! * 1000).toInt()
        sendTimeoutUnit.captured shouldBe TimeUnit.MILLISECONDS
        blockIfQueueFull.captured shouldBe randomConfig.blockIfQueueFull
      }
    },
)
