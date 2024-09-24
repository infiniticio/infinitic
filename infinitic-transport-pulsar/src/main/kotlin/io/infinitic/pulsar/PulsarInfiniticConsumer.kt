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

import io.infinitic.common.data.MillisInstant
import io.infinitic.common.messages.Envelope
import io.infinitic.common.messages.Message
import io.infinitic.common.transport.EventListenerSubscription
import io.infinitic.common.transport.InfiniticConsumer
import io.infinitic.common.transport.MainSubscription
import io.infinitic.common.transport.Subscription
import io.infinitic.common.transport.consumers.ConsumerSharedProcessor
import io.infinitic.common.transport.consumers.ConsumerUniqueProcessor
import io.infinitic.pulsar.client.InfiniticPulsarClient
import io.infinitic.pulsar.config.PulsarConsumerConfig
import io.infinitic.pulsar.consumers.PulsarTransportConsumer
import io.infinitic.pulsar.consumers.PulsarTransportMessage
import io.infinitic.pulsar.resources.PulsarResources
import io.infinitic.pulsar.resources.defaultName
import io.infinitic.pulsar.resources.defaultNameDLQ
import io.infinitic.pulsar.resources.schema
import io.infinitic.pulsar.resources.type
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

  override suspend fun <S : Message> start(
    subscription: Subscription<S>,
    entity: String,
    handler: suspend (S, MillisInstant) -> Unit,
    beforeDlq: (suspend (S?, Exception) -> Unit)?,
    concurrency: Int,
  ) {
    // Retrieves the name the topic, If the topic doesn't exist yet, create it.
    val topicName = with(pulsarResources) {
      subscription.topic.forEntity(entity, true, checkConsumer = false)
    }

    // Retrieves the name the DLQ topic, If the topic doesn't exist yet, create it.
    val topicDLQName = with(pulsarResources) {
      subscription.topic.forEntityDLQ(entity, true)
    }

    fun deserialize(message: PulsarTransportMessage<Envelope<out S>>): S =
        message.toPulsarMessage().value.message()

    suspend fun beforeNegativeAcknowledgement(
      message: PulsarTransportMessage<Envelope<out S>>,
      deserialized: S?,
      cause: Exception
    ) {
      if (message.redeliveryCount == pulsarConsumerConfig.maxRedeliverCount) {
        beforeDlq?.let { it(deserialized, cause) }
      }
    }

    fun buildConsumer(index: Int? = null) = getConsumer(
        schema = subscription.topic.schema,
        topic = topicName,
        topicDlq = topicDLQName,
        subscriptionName = subscription.name,
        subscriptionNameDlq = subscription.nameDLQ,
        subscriptionType = subscription.type,
        consumerName = entity + (index?.let { "-$it" } ?: ""),
    ).getOrThrow().let { PulsarTransportConsumer(it) }

    when (subscription.withKey) {
      true -> coroutineScope {
        val consumers = List(concurrency) { async { buildConsumer(it) } }.map { it.await() }

        List(concurrency) { index ->
          launch {
            val processor = ConsumerUniqueProcessor(
                consumers[index],
                ::deserialize,
                handler,
                ::beforeNegativeAcknowledgement,
            )
            processor.start()
          }
        }
      }

      false -> {
        val processor = ConsumerSharedProcessor(
            buildConsumer(),
            ::deserialize,
            handler,
            ::beforeNegativeAcknowledgement,
        )

        processor.start(concurrency)
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

  private fun <S : Envelope<out Message>> getConsumer(
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
}

