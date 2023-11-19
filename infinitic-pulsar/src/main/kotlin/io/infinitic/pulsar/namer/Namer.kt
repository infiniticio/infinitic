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
package io.infinitic.pulsar.namer

import org.apache.pulsar.client.api.PulsarClient
import org.apache.pulsar.client.api.PulsarClientException

open class Namer(open val pulsarClient: PulsarClient) {
  /** Useful to check the uniqueness of a connected producer's name or to provide a unique name */
  fun getName(
    topic: String,
    proposedName: String?
  ): Result<String> {
    val producer = try {
      pulsarClient
          .newProducer()
          .topic(topic)
          .also {
            if (proposedName != null) {
              it.producerName(proposedName)
            }
          }
          .create()
    } catch (e: PulsarClientException) {
      // if the producer name is already taken
      // the exception will be PulsarClientException.ProducerBusyException
      return Result.failure(e)
    }
    producer.close()

    return Result.success(producer.producerName)
  }
}
