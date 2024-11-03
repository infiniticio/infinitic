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

import io.github.oshai.kotlinlogging.KLogger
import io.infinitic.common.data.MillisInstant
import io.infinitic.common.messages.Envelope
import io.infinitic.common.messages.Message
import io.infinitic.common.transport.BatchProcessorConfig
import io.infinitic.common.transport.EventListenerSubscription
import io.infinitic.common.transport.MainSubscription
import io.infinitic.common.transport.Subscription
import io.infinitic.common.transport.config.BatchConfig
import io.infinitic.common.transport.consumers.startAsync
import io.infinitic.common.transport.interfaces.InfiniticConsumer
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

  context(KLogger)
  override suspend fun <M : Message> buildConsumers(
    subscription: Subscription<M>,
    entity: String,
    batchConfig: BatchConfig?,
    occurrence: Int?
  ): List<PulsarTransportConsumer<M>> {
    // Retrieve the name of the topic and of the DLQ topic
    // Create them if they do not exist.
    val (topicName, topicDLQName) = getOrCreateTopics(subscription, entity)

    return coroutineScope {
      List(occurrence ?: 1) { index ->
        async {
          val consumerName = entity + (occurrence?.let { "-${index + 1}" } ?: "")
          debug { "Creating consumer '${consumerName}' for $topicName" }
          getConsumer(
              schema = subscription.topic.schema,
              topic = topicName,
              topicDlq = topicDLQName,
              subscriptionName = subscription.name,
              subscriptionNameDlq = subscription.nameDLQ,
              subscriptionType = subscription.type,
              consumerName = consumerName,
              batchConfig = batchConfig,
          ).onSuccess {
            trace { "Consumer '${consumerName}' created for $topicName" }
          }
        }
      }.map { deferred ->
        deferred.await()
            .getOrThrow() // failed synchronously
            .let {
              PulsarTransportConsumer(
                  subscription.topic,
                  it,
                  pulsarConsumerConfig.getMaxRedeliverCount(),
              )
            }
      }
    }
  }

  context(CoroutineScope, KLogger)
  override suspend fun <S : Message> startAsync(
    subscription: Subscription<S>,
    entity: String,
    batchConfig: BatchConfig?,
    concurrency: Int,
    processor: suspend (S, MillisInstant) -> Unit,
    beforeDlq: (suspend (S, Exception) -> Unit)?,
    batchProcessorConfig: (suspend (S) -> BatchProcessorConfig?)?,
    batchProcessor: (suspend (List<S>, List<MillisInstant>) -> Unit)?
  ): Job {

    return when (subscription.withKey) {
      true -> {
        // multiple consumers with unique processing
        val consumers = buildConsumers(subscription, entity, batchConfig, concurrency)
        launch {
          repeat(concurrency) { index ->
            consumers[index].startAsync(
                batchConfig,
                concurrency = 1,
                { it.deserialize() },
                processor,
                beforeDlq,
                batchProcessorConfig,
                batchProcessor,
            )
          }
        }
      }

      false -> {
        // unique consumer with parallel processing
        val consumer = buildConsumer(subscription, entity, batchConfig)
        consumer.startAsync(
            batchConfig,
            concurrency,
            { it.deserialize() },
            processor,
            beforeDlq,
            batchProcessorConfig,
            batchProcessor,
        )
      }
    }
  }

  /**
   * Retrieves the name of the topic and the DLQ topic for a given entity.
   * The topics are created if they do not exist.
   *
   * @param M The type of the message.
   * @param subscription The subscription containing topic information.
   * @param entity The entity for which the topic names are to be retrieved.
   * @return A pair containing the topic name and the DLQ topic name.
   */
  private suspend fun <M : Message> getOrCreateTopics(
    subscription: Subscription<M>,
    entity: String,
  ): Pair<String, String> = coroutineScope {
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
    batchConfig: BatchConfig?,
  ): Result<Consumer<S>> {
    val consumerDef = InfiniticPulsarClient.ConsumerDef(
        topic = topic,
        subscriptionName = subscriptionName, //  MUST be the same for all instances!
        subscriptionType = subscriptionType,
        consumerName = consumerName,
        batchReceivingConfig = batchConfig,
        pulsarConsumerConfig = pulsarConsumerConfig,
    )
    val consumerDefDlq = topicDlq?.let {
      InfiniticPulsarClient.ConsumerDef(
          topic = it,
          subscriptionName = subscriptionNameDlq, //  MUST be the same for all instances!
          subscriptionType = SubscriptionType.Shared,
          consumerName = "$consumerName-dlq",
          batchReceivingConfig = batchConfig,
          pulsarConsumerConfig = pulsarConsumerConfig,
      )
    }

    return client.newConsumer(schema, consumerDef, consumerDefDlq)
  }
}

