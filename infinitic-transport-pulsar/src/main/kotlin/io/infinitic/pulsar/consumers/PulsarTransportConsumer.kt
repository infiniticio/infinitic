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
package io.infinitic.pulsar.consumers

import io.infinitic.common.messages.Envelope
import io.infinitic.common.messages.Message
import io.infinitic.common.transport.TransportConsumer
import kotlinx.coroutines.future.await
import org.apache.pulsar.client.api.Consumer

class PulsarTransportConsumer<M : Message>(
  private val pulsarConsumer: Consumer<Envelope<M>>
) : TransportConsumer<PulsarTransportMessage<M>> {

  override suspend fun receive(): PulsarTransportMessage<M> =
      PulsarTransportMessage(pulsarConsumer.receiveAsync().await())

  override suspend fun negativeAcknowledge(message: PulsarTransportMessage<M>) {
    pulsarConsumer.negativeAcknowledge(message.pulsarMessageId)
  }

  override suspend fun acknowledge(message: PulsarTransportMessage<M>) {
    pulsarConsumer.acknowledgeAsync(message.pulsarMessageId).await()
  }
}
