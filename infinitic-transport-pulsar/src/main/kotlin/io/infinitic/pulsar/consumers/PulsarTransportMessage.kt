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

import io.infinitic.common.data.MillisInstant
import io.infinitic.common.messages.Envelope
import io.infinitic.common.messages.Message
import io.infinitic.common.transport.Topic
import io.infinitic.common.transport.interfaces.TransportMessage
import kotlinx.coroutines.future.await
import org.apache.pulsar.client.api.Consumer as PulsarConsumer
import org.apache.pulsar.client.api.Message as PulsarMessage

class PulsarTransportMessage<M : Message>(
  private val pulsarMessage: PulsarMessage<Envelope<M>>,
  private val pulsarConsumer: PulsarConsumer<Envelope<M>>,
  override val topic: Topic<M>,
  maxRedeliveryCount: Int
) : TransportMessage<M> {
  override val publishTime = MillisInstant(pulsarMessage.publishTime)
  override val messageId = pulsarMessage.messageId.toString()

  /**
   * Deserializes the message from the pulsarMessage into its original form.
   *
   * @return The deserialized message of type M.
   */
  override fun deserialize(): M = pulsarMessage.value.message()

  /**
   * Synchronously acknowledges that the message has been successfully processed.
   * This operation informs the PulsarConsumer that the message has been successfully processed
   * and can be removed from the subscription.
   */
  override suspend fun acknowledge() {
    pulsarConsumer.acknowledgeAsync(pulsarMessage).await()
  }

  /**
   * Processes a negative acknowledgment for the given message, indicating to the Pulsar consumer
   * that the message could not be processed successfully and should be redelivered.
   *
   * This method invokes the `negativeAcknowledge` method on the underlying Pulsar consumer, which handles the logic
   * for message redelivery based on the consumer's configuration and the message's redelivery count.
   */
  override suspend fun negativeAcknowledge() {
    pulsarConsumer.negativeAcknowledge(pulsarMessage)
  }

  /**
   * Indicates whether the message has been sent to the Dead Letter Queue (DLQ).
   *
   * This property is `true` if the number of redelivery attempts for the Pulsar message has reached
   * the maximum redelivery count allowed, as defined by `maxRedeliveryCount`.
   */
  override val hasBeenSentToDeadLetterQueue = (maxRedeliveryCount == pulsarMessage.redeliveryCount)
}
