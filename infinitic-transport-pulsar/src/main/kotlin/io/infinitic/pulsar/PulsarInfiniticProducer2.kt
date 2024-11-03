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
import io.infinitic.common.data.MillisDuration
import io.infinitic.common.messages.Envelope
import io.infinitic.common.messages.Message
import io.infinitic.common.transport.ClientTopic
import io.infinitic.common.transport.NamingTopic
import io.infinitic.common.transport.Topic
import io.infinitic.common.transport.config.BatchConfig
import io.infinitic.common.transport.interfaces.InfiniticProducer
import io.infinitic.pulsar.client.InfiniticPulsarClient
import io.infinitic.pulsar.config.PulsarProducerConfig
import io.infinitic.pulsar.resources.PulsarResources
import io.infinitic.pulsar.resources.envelope
import io.infinitic.pulsar.resources.initWhenProducing
import kotlinx.coroutines.future.await
import org.apache.pulsar.client.api.Producer
import org.apache.pulsar.client.api.PulsarClientException.AlreadyClosedException
import org.apache.pulsar.client.api.PulsarClientException.TopicDoesNotExistException
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit
import kotlin.reflect.KClass

class PulsarInfiniticProducer2(
  private val client: InfiniticPulsarClient,
  private val pulsarProducerConfig: PulsarProducerConfig,
  private val pulsarResources: PulsarResources,
  private val batchConfig: BatchConfig?
) : InfiniticProducer {

  private var suggestedName: String? = null

  override fun setName(name: String) {
    suggestedName = name
  }

  override suspend fun getName(): String {
    val namingTopic = with(pulsarResources) {
      NamingTopic.forEntity(null, init = true, checkConsumer = false)
    }
    // Get unique name
    return client.getUniqueName(namingTopic, suggestedName).getOrThrow()
  }

  override suspend fun <T : Message> internalSendTo(
    message: T,
    topic: Topic<out T>,
    after: MillisDuration
  ) = try {
    sendEnvelope(message, topic, after, message.key())
  } catch (e: Exception) {
    if (topic.canIgnore(e)) Unit
    else throw e
  }

  private suspend fun <T : Message> sendEnvelope(
    message: T,
    topic: Topic<out T>,
    after: MillisDuration,
    key: String? = null
  ): Unit = sendEnvelopeAsync(message, topic, after, key).await()

  private suspend fun <T : Message> sendEnvelopeAsync(
    message: T,
    topic: Topic<out T>,
    after: MillisDuration,
    key: String? = null
  ): CompletableFuture<Unit> {
    // get message wrapped into an envelope
    val envelope = topic.envelope(message)

    // get cached producer or create it
    val producer = getProducer(topic, message.entity(), envelope::class, key, batchConfig)
        .getOrElse { return CompletableFuture.failedFuture(it) }

    logger.trace { "Sending${if (after > 0) " after $after ms" else ""} to topic '${producer.topic}' with key '$key': '$envelope'" }

    return producer
        .newMessage()
        .value(envelope)
        .also {
          if (key != null) {
            it.key(key)
          }
          if (after > 0) {
            it.deliverAfter(after.millis, TimeUnit.MILLISECONDS)
          }
        }
        .sendAsync()
        // remove MessageId from the completed CompletableFuture
        .thenApply { }
  }

  private fun Topic<*>.canIgnore(e: Exception): Boolean = when (this) {
    // If response topic does not exist, it means the client has closed
    // If producer is already closed, it means that the topics existed, was used, but does not exist anymore
    // in those cases, we are ok not to send this message
    is ClientTopic -> (e is TopicDoesNotExistException || e is AlreadyClosedException)
    else -> false
  }

  private suspend fun <T : Message> getProducer(
    topic: Topic<out T>,
    entity: String,
    envelopeKClass: KClass<out Envelope<out T>>,
    key: String?,
    batchConfig: BatchConfig?
  ): Result<Producer<Envelope<out T>>> {
    val topicFullName = with(pulsarResources) {
      topic.forEntity(
          entity = entity,
          init = topic.initWhenProducing,
          checkConsumer = true,
      )
    }

    return client.getProducer(
        topicFullName,
        envelopeKClass,
        getName(),
        batchConfig,
        pulsarProducerConfig,
        key,
    )
  }

  companion object {
    val logger = KotlinLogging.logger {}
  }
}
