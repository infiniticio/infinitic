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

import io.infinitic.common.data.MillisDuration
import io.infinitic.common.messages.Message
import io.infinitic.common.transport.ClientTopic
import io.infinitic.common.transport.InfiniticProducer
import io.infinitic.common.transport.NamingTopic
import io.infinitic.common.transport.Topic
import io.infinitic.pulsar.producers.Producer
import io.infinitic.pulsar.resources.PulsarResources
import io.infinitic.pulsar.resources.envelope
import io.infinitic.pulsar.resources.initWhenProducing
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.apache.pulsar.client.api.PulsarClientException.AlreadyClosedException
import org.apache.pulsar.client.api.PulsarClientException.TopicDoesNotExistException

class PulsarInfiniticProducer(
  private val producer: Producer,
  private val pulsarResources: PulsarResources
) : InfiniticProducer {

  private var suggestedName: String? = null

  // If [suggestedName] is provided, we check that no other is connected with it
  // If [suggestedName] is not provided, Pulsar will provide a unique name
  private val uniqueName: String by lazy {
    runBlocking(Dispatchers.IO) {
      val namingTopic = with(pulsarResources) {
        NamingTopic.forEntity(null, init = true, checkConsumer = false)
      }
      // Get unique name
      producer.getUniqueName(namingTopic, suggestedName).getOrThrow()
    }
  }

  // (if set, must be done before sending the first message)
  override var name: String
    get() = uniqueName
    set(value) {
      suggestedName = value
    }

  override suspend fun <T : Message> internalSendTo(
    message: T,
    topic: Topic<T>,
    after: MillisDuration
  ) {
    val topicFullName = with(pulsarResources) {
      topic.forEntity(
          message.entity(),
          init = topic.initWhenProducing,
          checkConsumer = true,
      )
    }

    return try {
      producer.send(
          topic.envelope(message),
          after,
          topicFullName,
          name,
          key = message.key(),
      )
    } catch (e: Exception) {
      if (topic.canIgnore(e)) Unit
      else throw e
    }
  }

  private fun Topic<*>.canIgnore(e: Exception): Boolean = when (this) {
    // If response topic does not exist, it means the client closed
    // If producer is already closed, it means that the topics existed, was used, but does not exist anymore
    // in those cases, we are ok not to send this message
    is ClientTopic -> (e is TopicDoesNotExistException || e is AlreadyClosedException)
    else -> false
  }
}
