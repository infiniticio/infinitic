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

import io.infinitic.exceptions.clients.ExceptionAtInitialization
import org.apache.pulsar.client.api.PulsarClient
import org.apache.pulsar.client.api.PulsarClientException

/** Useful to check the uniqueness of a connected producer's name or to provide a unique name */
internal fun getProducerName(
  pulsarClient: PulsarClient,
  topic: String,
  name: String?
): String {
  val producer = try {
    pulsarClient
        .newProducer()
        .topic(topic)
        .also {
          if (name != null) {
            it.producerName(name)
          }
        }
        .create()
  } catch (e: PulsarClientException.ProducerBusyException) {
    System.err.print(
        "Another producer with name \"$name\" is already connected. Make sure to use a unique name.",
    )
    throw ExceptionAtInitialization(e)
  }
  producer.close()

  return producer.producerName
}
