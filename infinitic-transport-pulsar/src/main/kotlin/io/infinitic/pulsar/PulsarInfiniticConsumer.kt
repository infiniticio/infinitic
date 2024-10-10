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
package io.infinitic.pulsar

import io.github.oshai.kotlinlogging.KotlinLogging
import io.infinitic.common.data.MillisInstant
import io.infinitic.common.messages.Envelope
import io.infinitic.common.messages.Message
import io.infinitic.common.transport.BatchConfig
import io.infinitic.common.transport.EventListenerSubscription
import io.infinitic.common.transport.InfiniticConsumer
import io.infinitic.common.transport.MainSubscription
import io.infinitic.common.transport.Subscription
import io.infinitic.common.transport.TransportConsumer
import io.infinitic.common.transport.TransportMessage
import io.infinitic.common.transport.consumers.ProcessorConsumer
import io.infinitic.pulsar.client.InfiniticPulsarClient
import io.infinitic.pulsar.config.PulsarConsumerConfig
import io.infinitic.pulsar.consumers.PulsarTransportConsumer
import io.infinitic.pulsar.resources.PulsarResources
import io.infinitic.pulsar.resources.defaultName
import io.infinitic.pulsar.resources.defaultNameDLQ
import io.infinitic.pulsar.resources.schema
import io.infinitic.pulsar.resources.type
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import org.apache.pulsar.client.api.Consumer
import org.apache.pulsar.client.api.Schema
import org.apache.pulsar.client.api.SubscriptionType

class PulsarInfiniticConsumer(
  private val client: InfiniticPulsarClient,
  private val pulsarConsumerConfig: PulsarConsumerConfig,
  private val pulsarResources: PulsarResources,
) : InfiniticConsumer {

  override suspend fun <M : Message> buildConsumers(
    subscription: Subscription<M>,
    entity: String,
    occurrence: Int?
  ): List<TransportConsumer<out TransportMessage<M>>> {
    // Retrieve the name of the topic and of the DLQ topic
    // Create them if they do not exist.
    val (topicName, topicDLQName) = coroutineScope {
      val deferredTopic = async {
        with(pulsarResources) {
          subscription.topic.forEntity(entity, true, checkConsumer = false)
        }
      }
      val deferredTopicDLQ = async {
        with(pulsarResources) {
          subscription.topic.forEntityDLQ(entity, true)
        }
      }
      Pair(deferredTopic.await(), deferredTopicDLQ.await())
    }

    return coroutineScope {
      List(occurrence ?: 1) { index ->
        async {
          val consumerName = entity + (occurrence?.let { "-${index + 1}" } ?: "")
          logger.debug { "Creating consumer '${consumerName}' for $topicName" }
          getConsumer(
              schema = subscription.topic.schema,
              topic = topicName,
              topicDlq = topicDLQName,
              subscriptionName = subscription.name,
              subscriptionNameDlq = subscription.nameDLQ,
              subscriptionType = subscription.type,
              consumerName = consumerName,
          ).getOrThrow().let { PulsarTransportConsumer(it) }.also {
            logger.trace { "Consumer '${consumerName}' created for $topicName" }
          }
        }
      }.map { it.await() }
    }
  }


  context(CoroutineScope)
  override suspend fun <S : Message> startAsync(
    subscription: Subscription<S>,
    entity: String,
    concurrency: Int,
    process: suspend (S, MillisInstant) -> Unit,
    beforeDlq: (suspend (S?, Exception) -> Unit)?,
    batchConfig: (suspend (S) -> BatchConfig?)?,
    batchProcess: (suspend (List<S>, List<MillisInstant>) -> Unit)?
  ): Job {

    val loggedDeserialize: suspend (TransportMessage<S>) -> S = { message ->
      logger.debug { "Deserializing message: ${message.messageId}" }
      message.deserialize().also {
        logger.trace { "Deserialized message: ${message.messageId}" }
      }
    }

    val loggedHandler: suspend (S, MillisInstant) -> Unit = { message, publishTime ->
      logger.debug { "Processing $message" }
      process(message, publishTime)
      logger.trace { "Processed $message" }
    }

    val beforeNegativeAcknowledgement: suspend (TransportMessage<S>, S?, Exception) -> Unit =
        { message, deserialized, cause ->
          if (message.redeliveryCount == pulsarConsumerConfig.maxRedeliverCount) {
            beforeDlq?.let {
              logger.debug { "Processing beforeNegativeAcknowledgement for ${deserialized ?: message.messageId}" }
              it(deserialized, cause)
              logger.trace { "Processed beforeNegativeAcknowledgement for ${deserialized ?: message.messageId}" }
            }
          }
        }

    return when (subscription.withKey) {
      true -> {
        // build the consumers synchronously (but in parallel)
        val consumers = buildConsumers(subscription, entity, concurrency)
        launch {
          repeat(concurrency) { index ->
            val processor = ProcessorConsumer(consumers[index], beforeNegativeAcknowledgement)
            with(processor) {
              startAsync(1, loggedDeserialize, loggedHandler)
            }
          }
        }
      }

      false -> {
        // build the unique consumer synchronously
        val consumer = buildConsumer(subscription, entity)
        val processor = ProcessorConsumer(consumer, beforeNegativeAcknowledgement)
        with(processor) {
          startAsync(concurrency, loggedDeserialize, loggedHandler, batchConfig, batchProcess)
        }
      }
    }
  }

  private val Subscription<*>.name
    get() = when (this) {
      is MainSubscription -> defaultName
      is EventListenerSubscription -> name ?: defaultName
    }

  private val Subscription<*>.nameDLQ
    get() = when (this) {
      is MainSubscription -> defaultNameDLQ
      is EventListenerSubscription -> name?.let { "$it-dlq" } ?: defaultNameDLQ
    }

  private fun <S : Envelope<M>, M : Message> getConsumer(
    schema: Schema<S>,
    topic: String,
    topicDlq: String?,
    subscriptionName: String,
    subscriptionNameDlq: String,
    subscriptionType: SubscriptionType,
    consumerName: String,
  ): Result<Consumer<S>> {
    val consumerDef = InfiniticPulsarClient.ConsumerDef(
        topic = topic,
        subscriptionName = subscriptionName, //  MUST be the same for all instances!
        subscriptionType = subscriptionType,
        consumerName = consumerName,
        pulsarConsumerConfig = pulsarConsumerConfig,
    )
    val consumerDefDlq = topicDlq?.let {
      InfiniticPulsarClient.ConsumerDef(
          topic = it,
          subscriptionName = subscriptionNameDlq, //  MUST be the same for all instances!
          subscriptionType = SubscriptionType.Shared,
          consumerName = "$consumerName-dlq",
          pulsarConsumerConfig = pulsarConsumerConfig,
      )
    }

    return client.newConsumer(schema, consumerDef, consumerDefDlq)
  }

  companion object {
    val logger = KotlinLogging.logger {}
  }
}

