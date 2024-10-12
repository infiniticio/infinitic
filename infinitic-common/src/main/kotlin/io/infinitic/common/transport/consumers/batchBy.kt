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
import io.infinitic.common.transport.BatchConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * Batches items from the channel based on the provided batch configuration.
 * Each message is processed to determine its batch configuration,
 * and the messages with the same batch key are grouped together.
 *
 * @param I The type of message.
 * @param getBatchConfig A suspending function that returns the batch configuration for a given message.
 *                       If null, messages are not batched.
 * @return A channel that emits batched results wrapped in a [Result] containing
 *         either a [SingleMessage] or a [MultipleMessages] instance.
 */
context(CoroutineScope, KLogger)
fun <M : Any, I> Channel<Result<M, I>>.batchBy(
  getBatchConfig: suspend (I) -> BatchConfig?,
): Channel<OneOrMany<Result<M, I>>> {
  val callingScope: CoroutineScope = this@CoroutineScope

  // output channel where result after processing are sent
  val outputChannel = Channel<OneOrMany<Result<M, I>>>()

  debug { "batchBy: starting listening channel ${this@batchBy.hashCode()}" }

  // channels where messages are sent to be batched (one channel per key)
  val batchingMutex = Mutex()
  val batchingChannels = mutableMapOf<String, Channel<Result<M, I>>>()

  launch {
    // start a non cancellable scope
    withContext(NonCancellable) {

      // Create a new channel for batching
      // WARNING: the definition is here to use the NonCancellable scope for startBatching 😓
      suspend fun createAndStartBatchingChannel(
        maxMessages: Int,
        maxDuration: Long
      ): Channel<Result<M, I>> = Channel<Result<M, I>>().also {
        debug { "batchBy: adding producer to batching channel ${it.hashCode()}" }
        it.addProducer()
        trace { "batchBy: producer added to batching channel ${it.hashCode()}" }
        it.startBatching(maxMessages, maxDuration, outputChannel)
      }

      // Get or create a batch channel based on configuration
      suspend fun getBatchingChannel(config: BatchConfig): Channel<Result<M, I>> =
          batchingMutex.withLock {
            batchingChannels.getOrPut(config.batchKey) {
              createAndStartBatchingChannel(config.maxMessages, config.maxDuration.millis)
            }
          }

      debug { "batchBy: adding producer to output channel ${outputChannel.hashCode()}" }
      outputChannel.addProducer()
      trace { "batchBy: producer added to output channel ${outputChannel.hashCode()}" }
      // For batching channels, the addProducer method is called at creation
      while (true) {
        try {
          // the only way to quit this loop is to close the input channel
          // which is triggered by canceling the calling scope
          val result = receiveIfNotClose().also { trace { "batchBy: receiving $it" } } ?: break
          if (result.isFailure) {
            outputChannel.send(One(result.failure()))
          }
          if (result.isSuccess) {
            val batchConfig = try {
              getBatchConfig(result.value())
            } catch (e: Exception) {
              outputChannel.send(One(result.failure(e)))
              continue
            }
            when (batchConfig) {
              null -> outputChannel.send(One(result))
              else -> getBatchingChannel(batchConfig).send(result)
            }
          }
        } catch (e: Error) {
          warn(e) { "Error during batching, cancelling calling scope" }
          callingScope.cancel()
        }
      }
      // if the current scope is active, we do not close channels yet
      trace { "batchBy: exiting, removing producer from output channel ${outputChannel.hashCode()}" }
      outputChannel.removeProducer()
      batchingMutex.withLock {
        batchingChannels.forEach { (key, channel) ->
          trace { "batchBy: exited, producer removed from $key batching channel ${channel.hashCode()}" }
          channel.removeProducer()
        }
      }
    }
  }

  return outputChannel
}

