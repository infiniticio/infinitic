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
import io.infinitic.autoclose.autoClose
import io.infinitic.common.data.MillisInstant
import io.infinitic.common.messages.Message
import io.infinitic.common.transport.ClientTopic
import io.infinitic.common.transport.DelayedServiceExecutorTopic
import io.infinitic.common.transport.DelayedWorkflowTaskExecutorTopic
import io.infinitic.common.transport.InfiniticConsumerAsync
import io.infinitic.common.transport.ListenerSubscription
import io.infinitic.common.transport.MainSubscription
import io.infinitic.common.transport.Subscription
import io.infinitic.pulsar.consumers.Consumer
import io.infinitic.pulsar.resources.PulsarResources
import io.infinitic.pulsar.resources.defaultInitialPosition
import io.infinitic.pulsar.resources.defaultName
import io.infinitic.pulsar.resources.defaultNameDLQ
import io.infinitic.pulsar.resources.schema
import io.infinitic.pulsar.resources.type
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.apache.pulsar.client.api.SubscriptionInitialPosition
import java.util.concurrent.atomic.AtomicBoolean

class PulsarInfiniticConsumerAsync(
  private val consumer: Consumer,
  private val pulsarResources: PulsarResources,
  val shutdownGracePeriodInSeconds: Double
) : InfiniticConsumerAsync {

  override fun join() = consumer.join()

  private var isClosed: AtomicBoolean = AtomicBoolean(false)

  // See InfiniticWorker
  override var logName: String? = null
  private val logger by lazy { KotlinLogging.logger(logName ?: this::class.java.name) }

  private lateinit var clientName: String

  override fun close() {
    // we test if consumingScope is active, just in case
    // the user tries to manually close an already closed resource
    if (!isClosed.getAndSet(true)) {
      runBlocking {
        try {
          withTimeout((shutdownGracePeriodInSeconds * 1000L).toLong()) {
            // By cancelling the consumer coroutine, we interrupt the main loop of consumption
            logger.info { "Processing ongoing messages..." }
            consumer.cancel()
            consumer.join()
            logger.info { "All ongoing messages have been processed." }
            // delete client topic after all in-memory messages have been processed
            deleteClientTopics()
            // then close other resources (typically pulsar client & admin)
            autoClose()
          }
        } catch (e: TimeoutCancellationException) {
          logger.warn {
            "The grace period (${shutdownGracePeriodInSeconds}s) allotted to close was insufficient. " +
                "Some ongoing messages may not have been processed properly."
          }
        }
      }
    }
  }

  override suspend fun <S : Message> start(
    subscription: Subscription<S>,
    entity: String,
    handler: suspend (S, MillisInstant) -> Unit,
    beforeDlq: suspend (S?, Exception) -> Unit,
    concurrency: Int,
  ) {
    when (subscription.topic) {
      // we do nothing here, as WorkflowTaskExecutorTopic and ServiceExecutorTopic
      // do not need a distinct topic to handle delayed messages in Pulsar
      DelayedWorkflowTaskExecutorTopic, DelayedServiceExecutorTopic -> return
      // record client name to be able to delete topic at closing
      ClientTopic -> clientName = entity
      else -> Unit
    }

    lateinit var topicName: String
    lateinit var topicDLQName: String

    coroutineScope {
      // get name of topic, creates it if it does not exist yet
      launch {
        topicName = with(pulsarResources) {
          subscription.topic.forEntity(
              entity,
              init = true,
              checkConsumer = false,
          )
        }
      }

      // name of DLQ topic, creates it if it does not exist yet
      launch {
        topicDLQName = with(pulsarResources) { subscription.topic.forEntityDLQ(entity, true) }
      }
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
      is ListenerSubscription -> name ?: defaultName
      is MainSubscription -> defaultName
    }

  private val Subscription<*>.nameDLQ
    get() = when (this) {
      is ListenerSubscription -> name?.let { "$it-dlq" } ?: defaultNameDLQ
      is MainSubscription -> defaultNameDLQ
    }

  private val Subscription<*>.initialPosition
    get() = when (this) {
      is ListenerSubscription -> name?.let {
        SubscriptionInitialPosition.Earliest
      } ?: defaultInitialPosition

      is MainSubscription -> defaultInitialPosition
    }

  private suspend fun deleteClientTopics() {
    if (::clientName.isInitialized) coroutineScope {
      launch {
        val clientTopic = with(pulsarResources) { ClientTopic.fullName(clientName) }
        logger.debug { "Deleting client topic '$clientTopic'." }
        pulsarResources.deleteTopic(clientTopic)
            .onFailure { logger.warn(it) { "Unable to delete client topic '$clientTopic'." } }
            .onSuccess { logger.info { "Client topic '$clientTopic' deleted." } }
      }
      launch {
        val clientDLQTopic = with(pulsarResources) { ClientTopic.fullNameDLQ(clientName) }
        logger.debug { "Deleting client DLQ topic '$clientDLQTopic'." }
        pulsarResources.deleteTopic(clientDLQTopic)
            .onFailure { logger.warn(it) { "Unable to delete client DLQ topic '$clientDLQTopic'." } }
            .onSuccess { logger.info { "Client DLQ topic '$clientDLQTopic' deleted." } }
      }
    }
  }
}

