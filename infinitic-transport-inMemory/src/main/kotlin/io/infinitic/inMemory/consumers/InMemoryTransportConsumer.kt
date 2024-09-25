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
import io.infinitic.common.transport.TransportConsumer
import io.infinitic.inMemory.channels.DelayedMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.future.future
import java.util.concurrent.CompletableFuture

class InMemoryTransportConsumer<S : Message>(
  private val channel: Channel<S>
) : TransportConsumer<InMemoryTransportMessage<S>> {
  val scope = CoroutineScope(Dispatchers.IO)

  override fun receiveAsync(): CompletableFuture<InMemoryTransportMessage<S>> = scope.future {
    InMemoryTransportMessage(channel.receive())
  }

  override fun negativeAcknowledgeAsync(messages: List<InMemoryTransportMessage<S>>): CompletableFuture<Unit> =
      scope.future {}

  override fun negativeAcknowledgeAsync(message: InMemoryTransportMessage<S>): CompletableFuture<Unit> =
      scope.future {}

  override fun acknowledgeAsync(messages: List<InMemoryTransportMessage<S>>): CompletableFuture<Unit> =
      scope.future {}

  override fun acknowledgeAsync(message: InMemoryTransportMessage<S>): CompletableFuture<Unit> =
      scope.future {}
}

class InMemoryTransportDelayedConsumer<S : Message>(
  private val channel: Channel<DelayedMessage<S>>
) : TransportConsumer<InMemoryTransportMessage<S>> {
  val scope = CoroutineScope(Dispatchers.IO)

  override fun receiveAsync(): CompletableFuture<InMemoryTransportMessage<S>> = scope.future {
    channel.receive().let { message ->
      delay(message.after.millis)
      InMemoryTransportMessage(message.message)
    }
  }

  override fun negativeAcknowledgeAsync(messages: List<InMemoryTransportMessage<S>>): CompletableFuture<Unit> =
      scope.future {}

  override fun negativeAcknowledgeAsync(message: InMemoryTransportMessage<S>): CompletableFuture<Unit> =
      scope.future {}

  override fun acknowledgeAsync(messages: List<InMemoryTransportMessage<S>>): CompletableFuture<Unit> =
      scope.future {}

  override fun acknowledgeAsync(message: InMemoryTransportMessage<S>): CompletableFuture<Unit> =
      scope.future {}
}
