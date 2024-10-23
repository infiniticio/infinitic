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
import io.infinitic.common.transport.BatchConfig
import io.infinitic.common.transport.Subscription
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job

interface InfiniticConsumer {

  /**
   * Builds a list of transport consumers for a given subscription and entity.
   *
   * @param M The type of the messages to be consumed.
   * @param subscription The subscription from which to consume messages.
   * @param entity The entity associated with this consumer.
   * @param occurrence Optional parameter to specify the number of consumers to build.
   * @return A list of transport consumers for the specified subscription and entity.
   */
  context(KLogger)
  suspend fun <M : Message> buildConsumers(
    subscription: Subscription<M>,
    entity: String,
    occurrence: Int?
  ): List<TransportConsumer<out TransportMessage<M>>>

  context(KLogger)
  suspend fun <M : Message> buildConsumer(
    subscription: Subscription<M>,
    entity: String,
  ): TransportConsumer<out TransportMessage<M>> = buildConsumers(subscription, entity, null).first()

  /**
   * Starts asynchronous processing of messages for a given subscription.
   *
   * @param subscription The subscription from which to consume messages.
   * @param entity The entity associated with the consumer.
   * @param concurrency The number of concurrent coroutines for processing messages.
   * @param process A suspending function to process the deserialized message along with its publishing time.
   * @param beforeDlq An optional suspending function to execute before sending a message to DLQ.
   * @param batchConfig An optional suspending function to configure message batching.
   * @param batchProcess An optional suspending function to process batches of messages.
   * @return A Job representing the coroutine that runs the consuming process.
   */
  context(CoroutineScope, KLogger)
  suspend fun <S : Message> startAsync(
    subscription: Subscription<S>,
    entity: String,
    concurrency: Int,
    process: suspend (S, MillisInstant) -> Unit,
    beforeDlq: (suspend (S, Exception) -> Unit)? = null,
    batchConfig: (suspend (S) -> BatchConfig?)? = null,
    batchProcess: (suspend (List<S>, List<MillisInstant>) -> Unit)? = null
  ): Job

  /**
   * Starts processing messages from a given subscription.
   *
   * @param M The type of the messages to be consumed.
   * @param subscription The subscription from which to consume messages.
   * @param entity The entity associated with the consumer.
   * @param concurrency The number of concurrent coroutines for processing messages.
   * @param process A suspending function to process the deserialized message along with its publishing time.
   * @param beforeDlq An optional suspending function to execute before sending a message to DLQ.
   * @param batchConfig An optional suspending function to configure message batching.
   * @param batchProcess An optional suspending function to process batches of messages.
   */
  context(CoroutineScope, KLogger)
  suspend fun <M : Message> start(
    subscription: Subscription<M>,
    entity: String,
    concurrency: Int,
    process: suspend (M, MillisInstant) -> Unit,
    beforeDlq: (suspend (M, Exception) -> Unit)? = null,
    batchConfig: (suspend (M) -> BatchConfig?)? = null,
    batchProcess: (suspend (List<M>, List<MillisInstant>) -> Unit)? = null
  ) = startAsync(
      subscription, entity, concurrency, process, beforeDlq, batchConfig, batchProcess,
  ).join()
}
