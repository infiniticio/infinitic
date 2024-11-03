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
package io.infinitic.common.transport.interfaces

import io.github.oshai.kotlinlogging.KLogger
import io.infinitic.common.data.MillisInstant
import io.infinitic.common.messages.Message
import io.infinitic.common.transport.BatchProcessorConfig
import io.infinitic.common.transport.Subscription
import io.infinitic.common.transport.config.BatchConfig
import io.infinitic.common.transport.consumers.startAsync
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

interface InfiniticConsumerFactory {
  /**
   * Builds a list of transport consumers for processing messages from a given subscription.
   *
   * @param subscription The subscription from which to consume messages.
   * @param entity The entity associated with the consumer.
   * @param batchConfig An optional configuration for batching messages when receiving and sending.
   * @param occurrence An optional parameter indicating the number of occurrences.
   * @return A list of transport consumers for the specified subscription.
   */
  context(KLogger)
  suspend fun <M : Message> buildConsumers(
    subscription: Subscription<M>,
    entity: String,
    batchConfig: BatchConfig?,
    occurrence: Int?
  ): List<TransportConsumer<out TransportMessage<M>>>

  /**
   * Builds a transport consumer for processing messages from a given subscription.
   *
   * @param M The type of the messages to be consumed.
   * @param subscription The subscription from which to consume messages.
   * @param entity The entity associated with the consumer.
   * @param batchConfig An optional configuration for batching messages when receiving and sending.
   * @return A transport consumer for the specified subscription.
   */
  context(KLogger)
  suspend fun <M : Message> buildConsumer(
    subscription: Subscription<M>,
    entity: String,
    batchConfig: BatchConfig?
  ): TransportConsumer<out TransportMessage<M>> =
      buildConsumers(subscription, entity, batchConfig, null).first()

  /**
   * Starts asynchronous processing of messages for a given subscription.
   *
   * @param subscription The subscription from which to consume messages.
   * @param entity The entity associated with the consumer.
   * @param concurrency The number of concurrent coroutines for processing messages.
   * @param processor A function to process the deserialized message along with its publishing time.
   * @param beforeDlq An optional function to execute before sending a message to DLQ.
   * @param batchProcessorConfig An optional function to configure message batching when processing.
   * @param batchProcessor An optional function to process batches of messages.
   * @return A Job representing the coroutine that runs the consuming process.
   */
  context(CoroutineScope, KLogger)
  suspend fun <S : Message> startAsync(
    subscription: Subscription<S>,
    entity: String,
    batchConfig: BatchConfig? = null,
    concurrency: Int,
    processor: suspend (S, MillisInstant) -> Unit,
    beforeDlq: (suspend (S, Exception) -> Unit)? = null,
    batchProcessorConfig: (suspend (S) -> BatchProcessorConfig?)? = null,
    batchProcessor: (suspend (List<S>, List<MillisInstant>) -> Unit)? = null
  ): Job = when (subscription.withKey) {
    // multiple consumers with unique processing
    true -> {
      val consumers = buildConsumers(subscription, entity, batchConfig, concurrency)
      launch {
        repeat(concurrency) { index ->
          consumers[index].startAsync(
              batchReceivingConfig = batchConfig,
              concurrency = 1,
              processor = processor,
              beforeDlq = beforeDlq,
              batchProcessorConfig = batchProcessorConfig,
              batchProcessor = batchProcessor,
          )
        }
      }
    }

    // unique consumer with parallel processing
    false -> {
      val consumer = buildConsumer(subscription, entity, batchConfig)
      consumer.startAsync(
          batchReceivingConfig = batchConfig,
          concurrency = concurrency,
          processor = processor,
          beforeDlq = beforeDlq,
          batchProcessorConfig = batchProcessorConfig,
          batchProcessor = batchProcessor,
      )
    }
  }

  /**
   * Starts processing of messages from the given subscription.
   *
   * @param subscription The subscription from which to consume messages.
   * @param entity The entity associated with the consumer.
   * @param batchConfig An optional configuration for batching messages when receiving and sending.
   * @param concurrency The number of concurrent coroutines for processing messages.
   * @param process A function to process the deserialized message along with its publishing time.
   * @param beforeDlq An optional function to execute before sending a message to DLQ.
   * @param batchProcessorConfig An optional function to configure message batching when processing.
   * @param batchProcessor An optional function to process batches of messages.
   */
  context(CoroutineScope, KLogger)
  suspend fun <M : Message> start(
    subscription: Subscription<M>,
    entity: String,
    batchConfig: BatchConfig? = null,
    concurrency: Int,
    process: suspend (M, MillisInstant) -> Unit,
    beforeDlq: (suspend (M, Exception) -> Unit)? = null,
    batchProcessorConfig: (suspend (M) -> BatchProcessorConfig?)? = null,
    batchProcessor: (suspend (List<M>, List<MillisInstant>) -> Unit)? = null
  ) = startAsync(
      subscription,
      entity,
      batchConfig,
      concurrency,
      process,
      beforeDlq,
      batchProcessorConfig,
      batchProcessor,
  ).join()
}
