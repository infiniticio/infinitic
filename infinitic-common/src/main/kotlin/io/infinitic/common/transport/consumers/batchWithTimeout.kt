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
package io.infinitic.common.transport.consumers

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ChannelResult
import kotlinx.coroutines.selects.onTimeout
import kotlinx.coroutines.selects.select

/**
 * Collects messages from this channel into a batch until either the given maximum number of messages is collected
 * or the specified timeout duration elapses.
 *
 * @param maxMessages The maximum number of messages to include in the batch.
 * @param maxMillis The maximum duration (in milliseconds) to wait for messages before returning the batch.
 * @return A BatchResult containing the collected messages and a flag indicating if the channel is still open.
 */
@OptIn(ExperimentalCoroutinesApi::class)
suspend fun <S> Channel<S>.batchWithTimeout(
  maxMessages: Int,
  maxMillis: Long
): BatchResult<S> {
  val endTime = System.currentTimeMillis() + maxMillis
  // isActive becomes false after timeout
  var isActive = true
  // isOpen becomes false if the channel is closed
  var isOpen = true

  val messages = mutableListOf<S>()

  while (isActive && isOpen && messages.size < maxMessages) {
    // Use select to handle both timeout and receive without message loss
    select {
      onTimeout(endTime - System.currentTimeMillis()) {
        isActive = false
      }

      onReceiveCatching { result: ChannelResult<S> ->
        when {
          result.isClosed -> isOpen = false
          else -> messages.add(result.getOrThrow())
        }
      }
    }
  }

  return BatchResult(messages, isOpen)
}

/**
 * Represents the result of collecting messages into a batch.
 *
 * @param S The type of the messages being collected.
 * @param messages The list of messages collected in the batch.
 * @param isChannelOpen A flag indicating if the source channel is still open after collecting the batch.
 */
data class BatchResult<S>(
  val messages: List<S>,
  val isChannelOpen: Boolean,
)
