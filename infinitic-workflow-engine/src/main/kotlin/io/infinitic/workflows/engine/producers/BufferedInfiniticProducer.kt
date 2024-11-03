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

package io.infinitic.workflows.engine.producers

import io.infinitic.common.data.MillisDuration
import io.infinitic.common.messages.Message
import io.infinitic.common.transport.Topic
import io.infinitic.common.transport.interfaces.InfiniticProducer
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class BufferedInfiniticProducer(val producer: InfiniticProducer) : InfiniticProducer {
  private val buffer = mutableListOf<Letter<*>>()
  private val mutex = Mutex()

  override suspend fun getProducerName(): String = producer.getProducerName()

  override fun setSuggestedName(name: String) = producer.setSuggestedName(name)

  override suspend fun <T : Message> internalSendTo(
    message: T,
    topic: Topic<out T>,
    after: MillisDuration
  ) {
    mutex.withLock {
      buffer.add(Letter(message, topic, after))
    }
  }

  suspend fun flush() = mutex.withLock {
    coroutineScope {
      buffer.forEach {
        launch {
          with(producer) {
            it.message.sendTo(it.topic, it.after)
          }
        }
      }
    }
    buffer.clear()
  }
}

private data class Letter<T : Message>(
  val message: T,
  val topic: Topic<out T>,
  val after: MillisDuration
)
