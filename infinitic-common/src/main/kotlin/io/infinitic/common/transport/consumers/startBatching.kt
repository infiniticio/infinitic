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
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch

/**
 * Starts batching messages received from the input channel and sends them to an output channel.
 * Messages are batched based on either the maximum number of messages or the maximum duration.
 *
 * If the input channel is closed, all ongoing batches are completed and sent,
 * then the output channel is closed
 *
 * @param maxMessages The maximum number of messages to include in a batch.
 * @param maxMillis The maximum duration (in milliseconds) to wait for messages to form a batch.
 * @param outputChannel The channel to send the batched messages to. Defaults to a new channel.
 */
context(CoroutineScope, KLogger)
fun <T, M> Channel<Result<T, M>>.startBatching(
  maxMessages: Int,
  maxMillis: Long,
  outputChannel: Channel<Result<List<T>, List<M>>> = Channel(),
): Channel<Result<List<T>, List<M>>> {
  launch {
    var isOpen = true
    outputChannel.addProducer("startBatching")
    while (isOpen) {
      // the only way to quit this loop is to close the input channel
      val first = receiveIfNotClose() ?: break
      trace { "batching: receiving first $first " }
      val nextBatch = buildList {
        add(first)
        val result = batchWithTimeout(maxMessages - 1, maxMillis)
        result.messages.forEach { add(it) }
        isOpen = result.isChannelOpen
      }
      val out = Result.success(nextBatch.map { it.message }, nextBatch.map { it.data })
      debug { "batching: sending $out" }
      outputChannel.send(out)
      trace { "batching: sent $out" }
    }
    outputChannel.removeProducer("startBatching")
  }
  return outputChannel
}
