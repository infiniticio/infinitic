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

import io.infinitic.common.transport.interfaces.TransportConsumer
import io.infinitic.common.transport.interfaces.TransportMessage
import io.infinitic.common.transport.logged.LoggerWithCounter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.coroutines.cancellation.CancellationException

/**
 * Starts receiving messages from the provided transport consumer and processes them in a coroutine.
 * The received messages are sent to the specified output channel as `Result` objects.
 *
 * @param consumer The transport consumer from which messages will be received.
 * @param outputChannel The channel to which received messages will be sent as `Result` objects.
 *                      If not provided, a new channel is created and used.
 * @return The channel to which received messages are sent.
 */
context(LoggerWithCounter)
fun <T : TransportMessage<M>, M> CoroutineScope.startReceiving(
  consumer: TransportConsumer<out T>,
  outputChannel: Channel<Result<T, T>> = createChannel()
): Channel<Result<T, T>> {

  debug { "Starting producing on channel ${outputChannel.hashCode()} from ${consumer.name}" }

  launch {
    outputChannel.addProducer("startReceiving")

    while (isActive) {
      try {
        val msg = consumer.receive()
        withContext(NonCancellable) {
          trace { "Received from ${consumer.name}: $msg " }
          // counter
          incr()
          outputChannel.send(Result.success(msg, msg))
        }
      } catch (_: CancellationException) {
        // do nothing, will exit if calling scope is not active anymore
      } catch (e: Exception) {
        warn(e) { "Exception when receiving message from $this" }
      } catch (e: Error) {
        warn(e) { "Error when receiving message from $this" }
        // canceling current scope (warning scope is different from inside launch)
        this@CoroutineScope.cancel()
      }
    }
    withContext(NonCancellable) {
      outputChannel.removeProducer("startReceiving")
    }
  }

  return outputChannel
}

/**
 * Starts batch receiving of transport messages from the specified consumer and sends the received
 * batches to an output channel.
 *
 * @param consumer The consumer that will be used to receive the transport messages.
 * @param outputChannel The channel where the received batches will be sent.
 *                      If not provided, a new channel is created and used.
 * @return The output channel where the batches of transport messages are sent.
 */
context(LoggerWithCounter)
fun <T : TransportMessage<M>, M> CoroutineScope.startBatchReceiving(
  consumer: TransportConsumer<out T>,
  outputChannel: Channel<Result<List<T>, List<T>>> = createBatchChannel()
): Channel<Result<List<T>, List<T>>> {

  debug { "starting batch producing on channel ${outputChannel.hashCode()} from ${consumer.name}" }

  launch {
    outputChannel.addProducer("startBatchReceiving")

    while (isActive) {
      try {
        val batch = consumer.batchReceive()
        withContext(NonCancellable) {
          trace { "Batch (${batch.size}) received from ${consumer.name}: $batch" }
          // counter
          incr(batch.size)
          outputChannel.send(Result.success(batch, batch))
        }
      } catch (_: CancellationException) {
        // do nothing, will exit if calling scope is not active anymore
      } catch (e: Exception) {
        warn(e) { "Exception when receiving message from $this" }
      } catch (e: Error) {
        warn(e) { "Error when receiving message from $this" }
        // canceling current scope (warning scope is different from inside launch)
        this@CoroutineScope.cancel()
      }
    }
    withContext(NonCancellable) {
      outputChannel.removeProducer("startBatchReceiving")
    }
  }

  return outputChannel
}
