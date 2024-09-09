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
import io.infinitic.common.messages.Message
import io.infinitic.common.transport.EventListenerSubscription
import io.infinitic.common.transport.InfiniticConsumer
import io.infinitic.common.transport.MainSubscription
import io.infinitic.common.transport.RetryServiceExecutorTopic
import io.infinitic.common.transport.RetryWorkflowExecutorTopic
import io.infinitic.common.transport.Subscription
import io.infinitic.pulsar.consumers.Consumer
import io.infinitic.pulsar.resources.PulsarResources
import io.infinitic.pulsar.resources.defaultInitialPosition
import io.infinitic.pulsar.resources.defaultName
import io.infinitic.pulsar.resources.defaultNameDLQ
import io.infinitic.pulsar.resources.schema
import io.infinitic.pulsar.resources.type
import org.apache.pulsar.client.api.SubscriptionInitialPosition

class PulsarInfiniticConsumer(
  private val consumer: Consumer,
  private val pulsarResources: PulsarResources,
  val shutdownGracePeriodSeconds: Double
) : InfiniticConsumer {

  override suspend fun <S : Message> start(
    subscription: Subscription<S>,
    entity: String,
    handler: suspend (S, MillisInstant) -> Unit,
    beforeDlq: (suspend (S?, Exception) -> Unit)?,
    concurrency: Int,
  ) {
    when (subscription.topic) {
      // we do nothing here, as WorkflowTaskExecutorTopic and ServiceExecutorTopic
      // do not need a distinct topic to handle delayed messages in Pulsar
      RetryWorkflowExecutorTopic, RetryServiceExecutorTopic -> return

      else -> Unit
    }

    // Retrieves the name the topic, If the topic doesn't exist yet, create it.
    val topicName = with(pulsarResources) {
      subscription.topic.forEntity(entity, true, checkConsumer = false)
    }

    // Retrieves the name the DLQ topic, If the topic doesn't exist yet, create it.
    val topicDLQName = with(pulsarResources) {
      subscription.topic.forEntityDLQ(entity, true)
    }

    consumer.startListening(
        handler = handler,
        beforeDlq = beforeDlq,
        schema = subscription.topic.schema,
        topic = topicName,
        topicDlq = topicDLQName,
        subscriptionName = subscription.name,
        subscriptionNameDlq = subscription.nameDLQ,
        subscriptionType = subscription.type,
        subscriptionInitialPosition = subscription.initialPosition,
        consumerName = entity,
        concurrency = concurrency,
    )
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

  private val Subscription<*>.initialPosition
    get() = when (this) {
      is MainSubscription -> defaultInitialPosition
      is EventListenerSubscription -> name?.let { SubscriptionInitialPosition.Earliest }
        ?: defaultInitialPosition
    }
}

