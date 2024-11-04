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
package io.infinitic.common.transport.logged

import io.github.oshai.kotlinlogging.KLogger
import io.infinitic.common.data.MillisDuration
import io.infinitic.common.messages.Message
import io.infinitic.common.transport.Topic
import io.infinitic.common.transport.interfaces.InfiniticProducer

class LoggedInfiniticProducer(
  private val logger: KLogger,
  private val producer: InfiniticProducer,
) : InfiniticProducer {

  override val emitterName by lazy { producer.emitterName }

  override suspend fun <T : Message> internalSendTo(
    message: T,
    topic: Topic<out T>,
    after: MillisDuration
  ) {
    logSending(message, after, topic)
    with(producer) { internalSendTo(message, topic, after) }
    logSent(message, topic)
  }

  private fun logSending(message: Message, after: MillisDuration, topic: Topic<*>) {
    logger.debug {
      formatLog(
          message.id(),
          after.sending + " to ${topic::class.java.simpleName}:",
          message,
      )
    }
  }

  private fun logSent(message: Message, topic: Topic<*>) {
    logger.trace { formatLog(message.id(), "Sent to ${topic::class.java.simpleName}:", message) }
  }

  private val MillisDuration?.sending
    get() = when {
      this == null || this <= 0 -> "Sending"
      else -> "After $this, sending"
    }
}
