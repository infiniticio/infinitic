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
import io.infinitic.common.transport.InfiniticProducerAsync
import io.infinitic.common.transport.NamingTopic
import io.infinitic.common.transport.Topic
import io.infinitic.pulsar.producers.Producer
import io.infinitic.pulsar.resources.PulsarResources
import io.infinitic.pulsar.resources.envelope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import java.util.concurrent.CompletableFuture

class PulsarInfiniticProducerAsync(
  private val producer: Producer,
  private val pulsarResources: PulsarResources
) : InfiniticProducerAsync {

  private var suggestedName: String? = null

  // If [suggestedName] is provided, we check that no other is connected with it
  // If [suggestedName] is not provided, Pulsar will provide a unique name
  private val uniqueName: String by lazy {
    runBlocking(Dispatchers.IO) {
      val namingTopic = with(pulsarResources) { NamingTopic.forMessage() }
      // Get unique name
      producer.getUniqueName(namingTopic, suggestedName).getOrThrow()
    }
  }

  // (if set, must be done before sending the first message)
  override var producerName: String
    get() = uniqueName
    set(value) {
      suggestedName = value
    }

  override suspend fun <T : Message> internalSendToAsync(
    message: T,
    topic: Topic<T>,
    after: MillisDuration
  ): CompletableFuture<Unit> {
    val topicFullName = with(pulsarResources) { topic.forMessage(message) }

    return producer.sendAsync(
        topic.envelope(message),
        after,
        topicFullName,
        producerName,
        key = message.key(),
    )
  }
}
