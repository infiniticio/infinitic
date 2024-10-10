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
import kotlinx.coroutines.withTimeoutOrNull

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
  debug { "batching: adding producer on output channel ${this.hashCode()} " }
  outputChannel.addProducer()
  trace { "batching: added producer on output channel ${this.hashCode()} " }
  while (isOpen) {
    // the only way to quit this loop is to close the input channel
    val firstResult = receiveIfNotClose()
        .also { trace { "batching: first receiving $it " } } ?: break
    val nextBatch = buildList {
      add(firstResult)
      withTimeoutOrNull(maxDuration) {
        while (size < maxMessages) {
          val result = receiveIfNotClose()
              .also { trace { "batching: next receiving $it" } }
          when (result) {
            null -> {
              isOpen = false
              break
            }

            else -> add(result)
          }
        }
      }
    }
    debug { "batching: sending ${Many(nextBatch)}" }
    outputChannel.send(Many(nextBatch))
    trace { "batching: sent ${Many(nextBatch)}" }
  }
  debug { "batching: closing" }
  outputChannel.removeProducer()
  trace { "batching: closed" }
}
