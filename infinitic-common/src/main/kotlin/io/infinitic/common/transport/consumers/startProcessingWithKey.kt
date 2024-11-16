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
import io.infinitic.common.data.MillisInstant
import io.infinitic.common.transport.config.BatchConfig
import io.infinitic.common.transport.config.maxMillis
import io.infinitic.common.transport.interfaces.TransportConsumer
import io.infinitic.common.transport.interfaces.TransportMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope

/**
 * Starts processing of TransportMessages with sharding based on message keys.
 *
 * @param consumer The transport consumer from which messages will be received.
 * @param concurrency The number of concurrent processing routines.
 * @param processor The processing function to handle the deserialized message and its publish time.
 * @param beforeDlq An optional callback invoked before sending the message to the DLQ (Dead Letter Queue).
 */
fun <T : TransportMessage<M>, M> CoroutineScope.startProcessingWithKey(
  logger: KLogger,
  consumer: TransportConsumer<T>,
  concurrency: Int,
  processor: (suspend (M, MillisInstant) -> Unit),
  beforeDlq: (suspend (M, Exception) -> Unit)? = null,
) = with(logger) {
  val processedChannel: Channel<Result<T, Unit>> = Channel()

  startReceivingAndShard(
      consumer = consumer,
      batchReceiving = false,
      concurrency = concurrency,
  ).forEach { messagesChannel ->
    messagesChannel
        .process(to = processedChannel) { transportMessage, _ ->
          processor(transportMessage.deserialize(), transportMessage.publishTime)
        }
  }

  repeat(concurrency) {
    processedChannel.acknowledge(beforeDlq)
  }
}

/**
 * Starts batch processing of TransportMessages with sharding based on message keys.
 *
 * @param consumer The transport consumer that provides messages to be processed.
 * @param concurrency The number of concurrent batches to process.
 * @param batchConfig Configuration for batching messages, including maximum message count and batch duration.
 * @param batchProcessor The suspending function that processes a list of deserialized message pairs and their publish times.
 * @param beforeDlq An optional suspending function that is called before sending a message to the Dead Letter Queue (DLQ).
 */
fun <T : TransportMessage<M>, M> CoroutineScope.startBatchProcessingWithKey(
  logger: KLogger,
  consumer: TransportConsumer<T>,
  concurrency: Int,
  batchConfig: BatchConfig,
  batchProcessor: (suspend (List<Pair<M, MillisInstant>>) -> Unit),
  beforeDlq: (suspend (M, Exception) -> Unit)? = null,
) = with(logger) {

  val processedChannel: Channel<Result<List<T>, Unit>> = Channel()

  startReceivingAndShard(
      consumer,
      batchReceiving = true,
      concurrency = concurrency,
  ).forEach { messagesChannel ->

    messagesChannel
        .batchBy(batchConfig.maxMessages, batchConfig.maxMillis)
        .process(to = processedChannel) { transportMessages, _ ->
          val messages =
              coroutineScope { transportMessages.map { async { it.deserialize() } }.awaitAll() }
          batchProcessor(messages.zip(transportMessages) { m, t -> m to t.publishTime })
        }
  }


  repeat(concurrency) {
    processedChannel.acknowledge(beforeDlq)
  }
}
