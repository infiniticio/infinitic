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
package io.infinitic.common.transport.interfaces

import io.infinitic.common.data.MillisInstant
import io.infinitic.common.transport.Topic


/**
 * Represents a message transported within the Infinitic system.
 *
 * @param M The type of the deserialized message.
 */
interface TransportMessage<out M> {
  /**
   * The timestamp when the message was published.
   */
  val publishTime: MillisInstant

  /**
   * The unique identifier of the message.
   */
  val messageId: String

  /**
   * The topic to which the message was published.
   */
  val topic: Topic<*>

  /**
   *
   */
  val key: String?

  /**
   * Deserializes the message into its original form.
   */
  suspend fun deserialize(): M

  /**
   * Acknowledges the given message.
   */
  suspend fun acknowledge()

  /**
   * Negatively acknowledges the given message.
   */
  suspend fun negativeAcknowledge()

  /**
   * This property reflects the state where the message has failed to process successfully
   * repeatedly, and the total count of negative acknowledgments has reached a predefined limit
   * after which the message will be sent to DLQ
   */
  val sentToDeadLetterQueue: Boolean
}
