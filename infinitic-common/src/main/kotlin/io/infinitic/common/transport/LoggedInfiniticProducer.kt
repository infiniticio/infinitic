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

import io.github.oshai.kotlinlogging.KotlinLogging
import io.infinitic.common.data.MillisDuration
import io.infinitic.common.messages.Message
import io.infinitic.common.topics.Topic
import kotlinx.coroutines.future.await

class LoggedInfiniticProducer(
  logName: String,
  private val producerAsync: InfiniticProducerAsync,
) : InfiniticProducer {

  private val logger = KotlinLogging.logger(logName)

  lateinit var id: String

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
      val idStr = if (::id.isInitialized) "Id $id - " else ""
      val afterStr = if (after != null && after > 0) "After $after, s" else "S"
      "$idStr${afterStr}ending $message"
    }
  }

  private fun logTrace(message: Message) {
    logger.trace {
      val idStr = if (::id.isInitialized) "Id $id - " else ""
      "${idStr}Sent $message"
    }
  }
}
