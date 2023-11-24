package io.infinitic.pulsar.consumers

import io.infinitic.common.fixtures.TestFactory
import io.infinitic.common.workflows.engine.messages.WorkflowEngineEnvelope
import io.infinitic.common.workflows.engine.messages.WorkflowEngineMessage
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import org.apache.pulsar.client.api.ConsumerBuilder
import org.apache.pulsar.client.api.ConsumerCryptoFailureAction
import org.apache.pulsar.client.api.DeadLetterPolicy
import org.apache.pulsar.client.api.PulsarClient
import org.apache.pulsar.client.api.Schema
import org.apache.pulsar.client.api.SubscriptionInitialPosition
import org.apache.pulsar.client.api.SubscriptionType
import org.apache.pulsar.client.impl.MessageIdImpl
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit
import org.apache.pulsar.client.api.Consumer as PulsarConsumer
import org.apache.pulsar.client.api.Message as PulsarMessage

class ConsumerTests :
  StringSpec(
      {
        // consumer properties slots
        val topic = slot<String>()
        val subscriptionName = slot<String>()
        val subscriptionType = slot<SubscriptionType>()
        val consumerName = slot<String>()
        val subscriptionInitialPosition = slot<SubscriptionInitialPosition>()
        val deadLetterPolicy = slot<DeadLetterPolicy>()
        val ackTimeout = slot<Long>()
        val ackTimeoutUnit = slot<TimeUnit>()
        val loadConf = slot<Map<String, String>>()
        val subscriptionProperties = slot<Map<String, String>>()
        val isAckReceiptEnabled = slot<Boolean>()
        val ackTimeoutTickTime = slot<Long>()
        val ackTimeoutTickTimeUnit = slot<TimeUnit>()
        val negativeAckRedeliveryDelay = slot<Long>()
        val negativeAckRedeliveryDelayUnit = slot<TimeUnit>()
        val defaultCryptoKeyReader = slot<String>()
        val cryptoFailureAction = slot<ConsumerCryptoFailureAction>()
        val receiverQueueSize = slot<Int>()
        val acknowledgmentGroupTime = slot<Long>()
        val acknowledgmentGroupTimeUnit = slot<TimeUnit>()
        val replicateSubscriptionState = slot<Boolean>()
        val maxTotalReceiverQueueSizeAcrossPartitions = slot<Int>()
        val priorityLevel = slot<Int>()
        val properties = slot<Map<String, String>>()
        val autoUpdatePartitions = slot<Boolean>()
        val autoUpdatePartitionsInterval = slot<Int>()
        val autoUpdatePartitionsIntervalUnit = slot<TimeUnit>()
        val enableBatchIndexAcknowledgment = slot<Boolean>()
        val maxPendingChunkedMessage = slot<Int>()
        val autoAckOldestChunkedMessageOnQueueFull = slot<Boolean>()
        val expireTimeOfIncompleteChunkedMessage = slot<Long>()
        val expireTimeOfIncompleteChunkedMessageUnit = slot<TimeUnit>()
        val startPaused = slot<Boolean>()

        // mocks
        val pulsarMessage = mockk<PulsarMessage<WorkflowEngineEnvelope>>() {
          every { messageId } returns MessageIdImpl(-1, -1, -1)
          every { value } returns TestFactory.random<WorkflowEngineEnvelope>()
        }

        fun getConsumer() = mockk<PulsarConsumer<WorkflowEngineEnvelope>> {
          every { receiveAsync() } returns CompletableFuture.completedFuture(pulsarMessage)
          every { unsubscribe() } returns Unit
          every { close() } returns Unit
        }

        fun getConsumerBuilder() = mockk<ConsumerBuilder<WorkflowEngineEnvelope>> {
          every { subscribe() } returns getConsumer()
          every { topic(capture(topic)) } returns this
          every { subscriptionType(capture(subscriptionType)) } returns this
          every { subscriptionName(capture(subscriptionName)) } returns this
          every { consumerName(capture(consumerName)) } returns this
          every { subscriptionInitialPosition(capture(subscriptionInitialPosition)) } returns this
          every { deadLetterPolicy(capture(deadLetterPolicy)) } returns this
          every { ackTimeout(capture(ackTimeout), capture(ackTimeoutUnit)) } returns this
          every { loadConf(capture(loadConf)) } returns this
          every { subscriptionProperties(capture(subscriptionProperties)) } returns this
          every { isAckReceiptEnabled(capture(isAckReceiptEnabled)) } returns this
          every {
            ackTimeoutTickTime(
                capture(ackTimeoutTickTime),
                capture(ackTimeoutTickTimeUnit),
            )
          } returns this
          every {
            negativeAckRedeliveryDelay(
                capture(negativeAckRedeliveryDelay),
                capture(negativeAckRedeliveryDelayUnit),
            )
          } returns this
          every { defaultCryptoKeyReader(capture(defaultCryptoKeyReader)) } returns this
          every { cryptoFailureAction(capture(cryptoFailureAction)) } returns this
          every { receiverQueueSize(capture(receiverQueueSize)) } returns this
          every {
            acknowledgmentGroupTime(
                capture(acknowledgmentGroupTime),
                capture(acknowledgmentGroupTimeUnit),
            )
          } returns this
          every { replicateSubscriptionState(capture(replicateSubscriptionState)) } returns this
          every {
            maxTotalReceiverQueueSizeAcrossPartitions(
                capture(
                    maxTotalReceiverQueueSizeAcrossPartitions,
                ),
            )
          } returns this
          every { priorityLevel(capture(priorityLevel)) } returns this
          every { properties(capture(properties)) } returns this
          every { autoUpdatePartitions(capture(autoUpdatePartitions)) } returns this
          every {
            autoUpdatePartitionsInterval(
                capture(autoUpdatePartitionsInterval),
                capture(autoUpdatePartitionsIntervalUnit),
            )
          } returns this
          every { enableBatchIndexAcknowledgment(capture(enableBatchIndexAcknowledgment)) } returns this
          every { maxPendingChunkedMessage(capture(maxPendingChunkedMessage)) } returns this
          every {
            autoAckOldestChunkedMessageOnQueueFull(
                capture(
                    autoAckOldestChunkedMessageOnQueueFull,
                ),
            )
          } returns this
          every {
            expireTimeOfIncompleteChunkedMessage(
                capture(expireTimeOfIncompleteChunkedMessage),
                capture(expireTimeOfIncompleteChunkedMessageUnit),
            )
          } returns this
          every { startPaused(capture(startPaused)) } returns this
        }


        val client = mockk<PulsarClient> {
          every { newConsumer(any<Schema<WorkflowEngineEnvelope>>()) } returns getConsumerBuilder()
        }

        "Configuration given should be applied to consumer" {
          val randomConfig = TestFactory.random<ConsumerConfig>()
          val randomTopic = TestFactory.random<String>()
          val randomTopicDlq = TestFactory.random<String>()
          val randomSubscriptionName = TestFactory.random<String>()
          val randomSubscriptionType = TestFactory.random<SubscriptionType>()
          val randomConsumerName = TestFactory.random<String>()

          val consumer = Consumer(client, randomConfig)
          // when

          consumer.createConsumer<WorkflowEngineMessage, WorkflowEngineEnvelope>(
              topic = randomTopic,
              topicDlq = randomTopicDlq,
              subscriptionName = randomSubscriptionName,
              subscriptionType = randomSubscriptionType,
              consumerName = randomConsumerName,
          )
          // then
          topic.captured shouldBe randomTopic
          subscriptionName.captured shouldBe randomSubscriptionName
          subscriptionType.captured shouldBe randomSubscriptionType
          when (subscriptionType.captured) {
            SubscriptionType.Key_Shared, SubscriptionType.Shared -> {
              deadLetterPolicy.captured shouldBe DeadLetterPolicy.builder()
                  .maxRedeliverCount(randomConfig.maxRedeliverCount)
                  .deadLetterTopic(randomTopicDlq)
                  .initialSubscriptionName("$randomSubscriptionName-dlq")
                  .build()
            }

            else -> Unit
          }
          consumerName.captured shouldBe randomConsumerName
          subscriptionInitialPosition.captured shouldBe SubscriptionInitialPosition.Earliest
          ackTimeout.captured shouldBe (randomConfig.ackTimeoutSeconds!! * 1000).toLong()
          ackTimeoutUnit.captured shouldBe TimeUnit.MILLISECONDS
          loadConf.captured shouldBe randomConfig.loadConf
          subscriptionProperties.captured shouldBe randomConfig.subscriptionProperties
          isAckReceiptEnabled.captured shouldBe randomConfig.isAckReceiptEnabled
          ackTimeoutTickTime.captured shouldBe (randomConfig.ackTimeoutTickTimeSeconds!! * 1000).toLong()
          ackTimeoutTickTimeUnit.captured shouldBe TimeUnit.MILLISECONDS
          negativeAckRedeliveryDelay.captured shouldBe (randomConfig.negativeAckRedeliveryDelaySeconds!! * 1000).toLong()
          negativeAckRedeliveryDelayUnit.captured shouldBe TimeUnit.MILLISECONDS
          defaultCryptoKeyReader.captured shouldBe randomConfig.defaultCryptoKeyReader
          cryptoFailureAction.captured shouldBe randomConfig.cryptoFailureAction
          receiverQueueSize.captured shouldBe randomConfig.receiverQueueSize
          acknowledgmentGroupTime.captured shouldBe (randomConfig.acknowledgmentGroupTimeSeconds!! * 1000).toLong()
          acknowledgmentGroupTimeUnit.captured shouldBe TimeUnit.MILLISECONDS
          replicateSubscriptionState.captured shouldBe randomConfig.replicateSubscriptionState
          maxTotalReceiverQueueSizeAcrossPartitions.captured shouldBe randomConfig.maxTotalReceiverQueueSizeAcrossPartitions
          priorityLevel.captured shouldBe randomConfig.priorityLevel
          properties.captured shouldBe randomConfig.properties
          autoUpdatePartitions.captured shouldBe randomConfig.autoUpdatePartitions
          autoUpdatePartitionsInterval.captured shouldBe (randomConfig.autoUpdatePartitionsIntervalSeconds!! * 1000).toInt()
          autoUpdatePartitionsIntervalUnit.captured shouldBe TimeUnit.MILLISECONDS
          enableBatchIndexAcknowledgment.captured shouldBe randomConfig.enableBatchIndexAcknowledgment
          maxPendingChunkedMessage.captured shouldBe randomConfig.maxPendingChunkedMessage
          autoAckOldestChunkedMessageOnQueueFull.captured shouldBe randomConfig.autoAckOldestChunkedMessageOnQueueFull
          expireTimeOfIncompleteChunkedMessage.captured shouldBe (randomConfig.expireTimeOfIncompleteChunkedMessageSeconds!! * 1000).toLong()
          expireTimeOfIncompleteChunkedMessageUnit.captured shouldBe TimeUnit.MILLISECONDS
          startPaused.captured shouldBe randomConfig.startPaused
        }
      },
  )
