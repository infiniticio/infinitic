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

interface TransportConsumer<T : TransportMessage<*>> {
  /**
   * Receives a transport message from the consumer.
   *
   * @return A message of type [T] received from the transport.
   */
  suspend fun receive(): T

  /**
   * Defines the maximum number of times a message can be redelivered
   * when processing messages from a transport consumer.
   */
  val maxRedeliveryCount: Int

  /**
   * Represents the name of the TransportConsumer. Used for Logging only
   */
  val name: String
}
