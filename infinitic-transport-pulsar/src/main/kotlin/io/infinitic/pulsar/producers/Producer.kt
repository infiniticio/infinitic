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
package io.infinitic.pulsar.producers

import io.github.oshai.kotlinlogging.KotlinLogging
import io.infinitic.common.data.MillisDuration
import io.infinitic.common.messages.Envelope
import io.infinitic.common.messages.Message
import io.infinitic.pulsar.client.PulsarInfiniticClient
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit

class Producer(
  val client: PulsarInfiniticClient,
  private val producerConfig: ProducerConfig
) {

  val logger = KotlinLogging.logger {}

  suspend fun getUniqueName(namerTopic: String, proposedName: String?) =
      client.getUniqueName(namerTopic, proposedName)

  fun sendAsync(
    envelope: Envelope<out Message>,
    after: MillisDuration,
    topic: String,
    producerName: String,
    key: String? = null
  ): CompletableFuture<Unit> {

    val producer = client
        .getProducer(topic, envelope::class, producerName, producerConfig, key)
        .getOrElse { return CompletableFuture.failedFuture(it) }

    logger.trace { "Sending${if (after > 0) " after $after ms" else ""} to topic '$topic' with key '$key': '$envelope'" }

    return producer
        .newMessage()
        .value(envelope)
        .also {
          if (key != null) {
            it.key(key)
          }
          if (after > 0) {
            it.deliverAfter(after.long, TimeUnit.MILLISECONDS)
          }
        }
        .sendAsync()
        // remove MessageId from the completed CompletableFuture
        .thenApply { }
  }
}
