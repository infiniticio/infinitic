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

import io.github.oshai.kotlinlogging.KLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.selects.onTimeout
import kotlinx.coroutines.selects.select

/**
 * Starts batching messages received from the input channel and sends them to an output channel.
 * Messages are batched based on either the maximum number of messages or the maximum duration.
 *
 * If the input channel is closed, all ongoing batches are completed and sent,
 * then the output channel is closed
 *
 * @param maxMessages The maximum number of messages to include in a batch.
 * @param maxDuration The maximum duration (in milliseconds) to wait for messages to form a batch.
 * @param outputChannel The channel to send the batched messages to. Defaults to a new channel.
 */
context(CoroutineScope, KLogger)
fun <S> Channel<S>.startBatching(
  maxMessages: Int,
  maxDuration: Long,
  outputChannel: Channel<in Many<S>>,
) = launch {
  var isOpen = true
  debug { "batching: adding producer on output channel ${this@startBatching.hashCode()} " }
  outputChannel.addProducer()
  trace { "batching: added producer on output channel ${this@startBatching.hashCode()} " }
  while (isOpen) {
    // the only way to quit this loop is to close the input channel
    val first = receiveIfNotClose() ?: break
    trace { "batching: receiving first $first " }
    val nextBatch = buildList {
      add(first)
      val result = batchWithTimeout(maxMessages - 1, maxDuration)
      result.messages.forEach { add(it) }
      isOpen = result.isOpen
    }
    debug { "batching: sending ${Many(nextBatch)}" }
    outputChannel.send(Many(nextBatch))
    trace { "batching: sent ${Many(nextBatch)}" }
  }
  debug { "batching: closing" }
  outputChannel.removeProducer()
  trace { "batching: closed" }
}

/**
 * Collects messages from this channel into a batch until either the given maximum number of messages is collected
 * or the specified timeout duration elapses.
 *
 * @param maxMessages The maximum number of messages to include in the batch.
 * @param timeout The maximum duration (in milliseconds) to wait for messages before returning the batch.
 * @return A BatchResult containing the collected messages and a flag indicating if the channel is still open.
 */
@OptIn(ExperimentalCoroutinesApi::class)
private suspend fun <S> Channel<S>.batchWithTimeout(
  maxMessages: Int,
  timeout: Long
): BatchResult<S> {
  val endTime = System.currentTimeMillis() + timeout
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

      onReceiveCatching { result ->
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
 * @param isOpen A flag indicating if the source channel is still open after collecting the batch.
 */
private data class BatchResult<S>(
  val messages: List<S>,
  val isOpen: Boolean,
)
