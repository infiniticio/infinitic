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

import io.infinitic.common.data.MillisInstant
import io.infinitic.common.messages.Message
import io.infinitic.common.transport.Topic
import io.infinitic.common.transport.interfaces.TransportMessage

class InMemoryTransportMessage<S : Message>(private val message: S, override val topic: Topic<S>) :
  TransportMessage<S> {
  private var hasBeenNegativelyAcknowledged = false

  override val messageId: String = message.messageId.toString()

  override val key: String = message.key().toString()

  override val publishTime: MillisInstant = MillisInstant.now()

  override suspend fun deserialize() = message

  override suspend fun negativeAcknowledge() {
    hasBeenNegativelyAcknowledged = true
  }

  override suspend fun acknowledge() {
    //  nothing to do
  }

  override val sentToDeadLetterQueue: Boolean = hasBeenNegativelyAcknowledged
}
