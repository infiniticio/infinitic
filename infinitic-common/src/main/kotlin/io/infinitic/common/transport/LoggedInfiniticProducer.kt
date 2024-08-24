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
package io.infinitic.common.transport

import io.github.oshai.kotlinlogging.KLogger
import io.infinitic.common.data.MillisDuration
import io.infinitic.common.messages.Message
import kotlinx.coroutines.future.await

class LoggedInfiniticProducer(
  private val logger: KLogger,
  private val producerAsync: InfiniticProducerAsync,
) : InfiniticProducer {

  override var name: String
    get() = producerAsync.producerName
    set(value) {
      producerAsync.producerName = value
    }

  override suspend fun <T : Message> T.sendTo(
    topic: Topic<T>,
    after: MillisDuration
  ) {
    logDebug(this)
    with(producerAsync) { sendToAsync(topic, after) }.await()
    logTrace(this)
  }

  private fun logDebug(message: Message, after: MillisDuration? = null) {
    logger.debug {
      when {
        after == null || after <= 0 -> "Sending"
        else -> "After $after, sending"
      } + " message: $message"
    }
  }

  private fun logTrace(message: Message) {
    logger.trace {
      "Message sent:    $message"
    }
  }
}
