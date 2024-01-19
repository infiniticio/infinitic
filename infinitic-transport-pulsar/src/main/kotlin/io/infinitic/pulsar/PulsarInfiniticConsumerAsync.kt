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
import io.infinitic.common.topics.ClientTopic
import io.infinitic.common.topics.DelayedServiceExecutorTopic
import io.infinitic.common.topics.DelayedWorkflowTaskExecutorTopic
import io.infinitic.common.topics.Topic
import io.infinitic.common.transport.InfiniticConsumerAsync
import io.infinitic.pulsar.consumers.Consumer
import io.infinitic.pulsar.resources.MainSubscription
import io.infinitic.pulsar.resources.PulsarResources
import io.infinitic.pulsar.resources.schema
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout

class PulsarInfiniticConsumerAsync(
  private val consumer: Consumer,
  private val pulsarResources: PulsarResources,
  val shutdownGracePeriodInSeconds: Double
) : InfiniticConsumerAsync {

  override var logName: String? = null
  override fun join() = consumer.join()

  // See InfiniticWorker
  private val logger by lazy { KotlinLogging.logger(logName ?: this::class.java.name) }

  private lateinit var clientName: String

  override fun close() {
    // we test if consumingScope is active, just in case
    // the user tries to manually close an already closed resource
    if (consumer.isActive) {
      runBlocking {
        try {
          withTimeout((shutdownGracePeriodInSeconds * 1000L).toLong()) {
            consumer.cancel()
            logger.info { "Processing ongoing messages..." }
            consumer.join()
            logger.info { "All ongoing messages have been processed." }
            // delete client topic only after
            deleteClientTopic()
          }
          // once the messages are processed, we can close other resources (pulsar client & admin)
          autoClose()
        } catch (e: TimeoutCancellationException) {
          logger.warn {
            "The grace period (${shutdownGracePeriodInSeconds}s) allotted to close was insufficient. " +
                "Some ongoing messages may not have been processed properly."
          }
        }
      }
    }
  }

  private suspend fun deleteClientTopic() {
    if (::clientName.isInitialized) {
      val clientTopic = with(pulsarResources) { ClientTopic.fullName(clientName) }
      logger.debug { "Deleting client topic '$clientTopic'." }
      pulsarResources.deleteTopic(clientTopic)
          .onFailure { logger.warn(it) { "Unable to delete client topic '$clientTopic'." } }
          .onSuccess { logger.info { "Client topic '$clientTopic' deleted." } }
    }
  }

  override suspend fun <S : Message> start(
    topic: Topic<S>,
    handler: suspend (S, MillisInstant) -> Unit,
    beforeDlq: suspend (S?, Exception) -> Unit,
    entity: String,
    concurrency: Int,
  ) {
    when (topic) {
      // we do nothing here, as WorkflowTaskExecutorTopic and ServiceExecutorTopic
      // do not need a distinct topic to handle delayed messages in Pulsar
      DelayedWorkflowTaskExecutorTopic, DelayedServiceExecutorTopic -> return
      // record client name to be able to delete topic at closing
      ClientTopic -> clientName = entity
      else -> Unit
    }

    startLoop(
        topic = topic,
        handler = handler,
        beforeDlq = beforeDlq,
        concurrency = concurrency,
        entity = entity,
    )
  }

  // Start a consumer on a topic, with concurrent executors
  private suspend fun <T : Message> startLoop(
    topic: Topic<T>,
    handler: suspend (T, MillisInstant) -> Unit,
    beforeDlq: suspend (T?, Exception) -> Unit,
    concurrency: Int,
    entity: String
  ) {

    lateinit var topicName: String
    lateinit var topicDLQName: String

    coroutineScope {
      // get name of topic, creates it if it does not exist yet
      launch { topicName = with(pulsarResources) { topic.fullName(entity) } }

      // name of DLQ topic, creates it if it does not exist yet
      launch { topicDLQName = with(pulsarResources) { topic.fullNameDLQ(entity) } }
    }

    consumer.startListening(
        handler = handler,
        beforeDlq = beforeDlq,
        schema = topic.schema,
        topic = topicName,
        topicDlq = topicDLQName,
        subscriptionName = MainSubscription.name(topic),
        subscriptionNameDlq = MainSubscription.nameDLQ(topic),
        subscriptionType = MainSubscription.type(topic),
        consumerName = entity,
        concurrency = concurrency,
    )
  }
}

