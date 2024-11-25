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
import io.infinitic.common.transport.interfaces.TransportMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * Batches messages received from the input channel based on a specified maximum number of
 * messages or maximum duration and sends the batched messages to an output channel.
 *
 * @param maxMessages The maximum number of messages to include in a batch.
 * @param maxMillis The maximum duration (in milliseconds) to wait for messages to form a batch.
 * @return A [Channel] containing the batched results.
 */
context(CoroutineScope, KLogger)
fun <T : TransportMessage<M>, M, D> Channel<Result<T, D>>.batchBy(
  maxMessages: Int,
  maxMillis: Long,
): Channel<Result<List<T>, List<D>>> {
  val callingScope: CoroutineScope = this@CoroutineScope

  // output channel where result after processing are sent
  val outputChannel = Channel<Result<List<T>, List<D>>>()

  debug { "batchBy: starting listening channel ${this@batchBy.hashCode()}" }

  // channels where messages are sent to be batched (one channel per key)
  val batchingMutex = Mutex()
  val batchingChannels = mutableMapOf<String, Channel<Result<T, D>>>()

  launch {
    // start a non cancellable scope
    withContext(NonCancellable) {

      // Get or create a batch channel based on configuration
      suspend fun getBatchingChannel(key: String): Channel<Result<T, D>> {
        // check if the channel already exists before using a lock
        batchingChannels[key]?.let { return it }

        return batchingMutex.withLock {
          batchingChannels.getOrPut(key) {
            // Create, register and start batching channel
            Channel<Result<T, D>> { warn { "Batch: Undelivered element $it" } }.apply {
              addProducer("batchBy")
              startBatching(maxMessages, maxMillis, outputChannel)
            }
          }
        }
      }

      outputChannel.addProducer("batchBy")
      // For batching channels, the addProducer method is called at creation
      while (true) {
        try {
          // the only way to quit this loop is to close the input channel
          // which is triggered by canceling the calling scope
          val result = receiveIfNotClose().also { trace { "batchBy: receiving $it" } } ?: break
          if (result.isFailure) {
            outputChannel.send(Result.failure(listOf(result.message), result.exception))
          }
          if (result.isSuccess) {
            getBatchingChannel("").send(result)
          }
        } catch (e: Exception) {
          warn(e) { "Exception during batching" }
          throw e
        } catch (e: Error) {
          warn(e) { "Error during batching, cancelling calling scope" }
          callingScope.cancel()
        }
      }
      // if the current scope is active, we do not close channels yet
      outputChannel.removeProducer("batchBy")
      batchingMutex.withLock {
        batchingChannels.forEach { (_, channel) ->
          channel.removeProducer("batchBy")
        }
      }
    }
  }

  return outputChannel
}

/**
 * Batches messages from an input channel based on a maximum number of messages or a maximum time
 * interval, then sends these batches to an output channel.
 *
 * @param maxMessages The maximum number of messages to include in a batch.
 * @param maxMillis The maximum duration (in milliseconds) to wait for messages to form a batch.
 * @param getBatchKey A suspend function that returns a key for grouping messages into batches.
 * @return A new channel that emits batched messages.
 */
context(CoroutineScope, KLogger)
fun <T : TransportMessage<M>, M> Channel<Result<List<T>, List<M>>>.batchBy(
  maxMessages: Int,
  maxMillis: Long,
  getBatchKey: suspend (M) -> String,
): Channel<Result<List<T>, List<M>>> {
  val callingScope: CoroutineScope = this@CoroutineScope

  // output channel where result after processing are sent
  val outputChannel = Channel<Result<List<T>, List<M>>>()

  debug { "batchBy: starting listening channel ${this@batchBy.hashCode()}" }

  // channels where messages are sent to be batched (one channel per key)
  val batchingMutex = Mutex()
  val batchingChannels = mutableMapOf<String, Channel<Result<T, M>>>()

  launch {
    // start a non cancellable scope
    withContext(NonCancellable) {

      // Get or create a batch channel based on configuration
      suspend fun getBatchingChannel(key: String): Channel<Result<T, M>> {
        // check if the channel already exists before using a lock
        batchingChannels[key]?.let { return it }

        return batchingMutex.withLock {
          batchingChannels.getOrPut(key) {
            Channel<Result<T, M>> { warn { "Batch: Undelivered element $it" } }.apply {
              addProducer("batchBy")
              startBatching(maxMessages, maxMillis, outputChannel)
            }
          }
        }
      }

      outputChannel.addProducer("batchBy")
      // For batching channels, the addProducer method is called at creation
      while (true) {
        try {
          // the only way to quit this loop is to close the input channel
          // which is triggered by canceling the calling scope
          val result = receiveIfNotClose().also { trace { "batchBy: receiving $it" } } ?: break
          if (result.isFailure) {
            outputChannel.send(result)
          }
          if (result.isSuccess) {
            val messagesByKey = result.message.zip(result.data).groupBy { getBatchKey(it.second) }

            when (messagesByKey.keys.size) {
              // if there is a unique key, then the batch is already done
              1 -> outputChannel.send(result)
              // else send messages to the same channel by key
              else -> messagesByKey.forEach { (key, messages) ->
                val batchingChannel = getBatchingChannel(key)
                messages.forEach { batchingChannel.send(Result.success(it.first, it.second)) }
              }
            }
          }
        } catch (e: Exception) {
          warn(e) { "Exception during batching" }
          throw e
        } catch (e: Error) {
          warn(e) { "Error during batching, cancelling calling scope" }
          callingScope.cancel()
        }
      }
      // if the current scope is active, we do not close channels yet
      outputChannel.removeProducer("batchBy")
      batchingMutex.withLock {
        batchingChannels.forEach { (_, channel) ->
          channel.removeProducer("batchBy")
        }
      }
    }
  }

  return outputChannel
}
