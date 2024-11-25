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

import io.infinitic.common.data.MillisInstant
import io.infinitic.common.transport.config.BatchConfig
import io.infinitic.common.transport.config.maxMillis
import io.infinitic.common.transport.interfaces.TransportConsumer
import io.infinitic.common.transport.interfaces.TransportMessage
import io.infinitic.common.transport.logged.LoggerWithCounter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope

/**
 * Starts processing of TransportMessages.
 *
 * @param consumer The transport consumer from which messages will be received.
 * @param concurrency The level of concurrency for message processing.
 * @param processor A suspend function that processes the deserialized message and its publish time.
 * @param beforeDlq An optional suspend function that is called before a message is sent to the
 *                  Dead Letter Queue if an exception occurs. Defaults to null.
 */
fun <T : TransportMessage<M>, M> CoroutineScope.startProcessingWithoutKey(
  logger: LoggerWithCounter,
  consumer: TransportConsumer<T>,
  concurrency: Int,
  processor: (suspend (M, MillisInstant) -> Unit),
  beforeDlq: (suspend (M, Exception) -> Unit)? = null,
) = with(logger) {
  startReceiving(consumer)
      .processWithoutKey(concurrency, processor, beforeDlq)
}

context(CoroutineScope, LoggerWithCounter)
fun <T : TransportMessage<M>, M> Channel<Result<T, T>>.processWithoutKey(
  concurrency: Int,
  processor: (suspend (M, MillisInstant) -> Unit),
  beforeDlq: (suspend (M, Exception) -> Unit)? = null,
) {
  val deserializedChannel: Channel<Result<T, M>> = Channel()
  repeat(concurrency) {
    this.process(deserializedChannel) { _, transportMessage ->
      transportMessage.deserialize()
    }
  }

  val processedChannel: Channel<Result<T, Unit>> = Channel()
  repeat(concurrency) {
    deserializedChannel.process(processedChannel) { transportMessage, message ->
      processor(message, transportMessage.publishTime)
    }
  }

  repeat(concurrency) {
    processedChannel.acknowledge(beforeDlq)
  }
}

/**
 * Starts batch processing of TransportMessages.
 *
 * @param consumer The transport consumer that will be used to receive messages.
 * @param concurrency The level of concurrency for processing batches.
 * @param batchConfig Configuration for batching, including maximum number of messages and timeout.
 * @param getBatchKey Optional function to provide a batch key based on the message.
 * @param processor Function to process a list of deserialized messages along with their publish times.
 * @param beforeDlq Optional function to execute before sending a message to the dead letter queue, in case of errors.
 */
fun <T : TransportMessage<M>, M> CoroutineScope.startBatchProcessingWithoutKey(
  logger: LoggerWithCounter,
  consumer: TransportConsumer<T>,
  concurrency: Int,
  batchConfig: BatchConfig,
  getBatchKey: (suspend (M) -> String)? = null,
  processor: suspend (List<Pair<M, MillisInstant>>) -> Unit,
  beforeDlq: (suspend (M, Exception) -> Unit)? = null,
) = with(logger) {
  startBatchReceiving(consumer)
      .processWithoutKey(concurrency, batchConfig, getBatchKey, processor, beforeDlq)
}

context(CoroutineScope, LoggerWithCounter)
fun <T : TransportMessage<M>, M> Channel<Result<List<T>, List<T>>>.processWithoutKey(
  concurrency: Int,
  batchConfig: BatchConfig,
  getBatchKey: (suspend (M) -> String)? = null,
  batchProcessor: suspend (List<Pair<M, MillisInstant>>) -> Unit,
  beforeDlq: (suspend (M, Exception) -> Unit)? = null,
) {
  // concurrently deserialize the transportMessages
  val deserializedChannel: Channel<Result<List<T>, List<M>>> = Channel()
  repeat(concurrency) {
    this.process(deserializedChannel) { messages, _ ->
      coroutineScope {
        messages.map { async { it.deserialize() } }.awaitAll()
      }
    }
  }

  // we do not batch in parallel to fill batches more quickly
  val batchedChannel = when (getBatchKey != null) {
    true -> deserializedChannel.batchBy(batchConfig.maxMessages, batchConfig.maxMillis, getBatchKey)
    false -> deserializedChannel
  }

  // concurrently process batches
  val processedChannel: Channel<Result<List<T>, Unit>> = Channel()
  repeat(concurrency) {
    batchedChannel.process(processedChannel) { transportMessages, messages ->
      batchProcessor(messages.zip(transportMessages.map { it.publishTime }))
    }
  }

  // concurrently (negatively) acknowledge transport messages
  repeat(concurrency) {
    processedChannel.acknowledge(beforeDlq)
  }
}

