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
import java.util.zip.CRC32
import kotlin.coroutines.cancellation.CancellationException

/**
 * Starts receiving messages and distributes (shards) them across multiple channels.
 *
 * @param batchReceiving Indicates whether to receive messages in batches or individually.
 * @param concurrency The number of concurrent shards to use for distributing the messages.
 * @return A list of channels, where each channel corresponds to a shard and contains the received messages.
 */
context(LoggerWithCounter)
internal fun <T : TransportMessage<M>, M> CoroutineScope.startReceivingAndShard(
  consumer: TransportConsumer<T>,
  batchReceiving: Boolean,
  concurrency: Int = 1,
): List<Channel<Result<T, T>>> {
  val shardChannels: List<Channel<Result<T, T>>> = List(concurrency) { createChannel() }

  launch {
    shardChannels.forEach { it.addProducer("startReceivingWithShard") }
    val size = shardChannels.size
    while (isActive) {
      try {
        when (batchReceiving) {
          true -> {
            val batch = consumer.batchReceive()
            withContext(NonCancellable) {
              trace { "Batch (${batch.size}) received from ${consumer.name}: $batch" }
              // counter
              incr(batch.size)
              batch.forEach {
                val shard = getShard(it.key!!, size)
                trace { "sending $it (key = ${it.key}) to shard $shard}" }
                shardChannels[shard].send(Result.success(it, it))
              }
            }
          }

          false -> {
            val msg = consumer.receive()
            withContext(NonCancellable) {
              trace { "Received from ${consumer.name}: $msg" }
              // counter
              incr()
              val shard = getShard(msg.key!!, size)
              shardChannels[shard].send(Result.success(msg, msg))
            }
          }
        }
      } catch (e: CancellationException) {
        // do nothing, will exit if calling scope is not active anymore
      } catch (e: Exception) {
        warn(e) { "Exception when receiving message from $this" }
      } catch (e: Error) {
        warn(e) { "Error when receiving message from $this" }
        // canceling current scope (warning scope is different from inside launch)
        // that's why we define the scope variable at the very beginning
        this@CoroutineScope.cancel()
      }
    }
    withContext(NonCancellable) {
      shardChannels.forEach { it.removeProducer("startReceivingWithShard") }
    }
  }

  return shardChannels
}

private val threadLocalCrc32 = ThreadLocal.withInitial { CRC32() }

internal fun getShard(input: String, totalShards: Int): Int {
  val crc32 = threadLocalCrc32.get().apply {
    reset() // Ensure CRC32 is reset before new computation
    update(input.toByteArray())
  }
  val hash = crc32.value
  return (hash and 0x7FFFFFFF).toInt() % totalShards // Ensure non-negative and get modulo
}
