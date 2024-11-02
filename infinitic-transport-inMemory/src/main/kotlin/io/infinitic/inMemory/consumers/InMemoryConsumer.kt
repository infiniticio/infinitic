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
package io.infinitic.inMemory.consumers

import io.infinitic.common.messages.Message
import io.infinitic.common.transport.Topic
import io.infinitic.common.transport.config.BatchConfig
import io.infinitic.common.transport.consumers.batchWithTimeout
import io.infinitic.common.transport.interfaces.TransportConsumer
import kotlinx.coroutines.channels.Channel

/**
 * An in-memory implementation of a transport consumer that consumes messages from a Kotlin Coroutine [Channel].
 *
 * @param S The type of message being consumed, which must extend the [Message] interface.
 * @property channel The channel from which messages are consumed.
 */
class InMemoryConsumer<S : Message>(
  private val topic: Topic<S>,
  private val batchReceivingConfig: BatchConfig?,
  private val channel: Channel<S>
) : TransportConsumer<InMemoryTransportMessage<S>> {

  override suspend fun receive() = InMemoryTransportMessage(channel.receive(), topic)

  override suspend fun batchReceive(): List<InMemoryTransportMessage<S>> = channel.batchWithTimeout(
      batchReceivingConfig!!.maxMessages,
      batchReceivingConfig.maxMillis,
  ).messages.map {
    InMemoryTransportMessage(it, topic)
  }

  override val maxRedeliveryCount = 1

  override val name: String = toString()
}
