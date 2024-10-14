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
package io.infinitic.common.transport

import io.github.oshai.kotlinlogging.KLogger
import io.infinitic.common.data.MillisInstant
import io.infinitic.common.messages.Message
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
   * Starts consuming messages from a given subscription and processes them using the provided handler.
   *
   * The CoroutineScope context is used to start the endless loop that listen for messages
   *
   * @return a job corresponding to the endless loop processing
   *
   * @param M The type of the messages to be consumed.
   * @param subscription The subscription from which to consume messages.
   * @param entity The entity associated with this consumer. (typically a service name or workflow name)
   * @param process The function to handle each consumed message and its publishing time.
   * @param beforeDlq An optional function to be executed before sending the message to the dead-letter queue (DLQ).
   * @param concurrency The number of concurrent message handlers to be used.
   */
  context(CoroutineScope, KLogger)
  suspend fun <M : Message> startAsync(
    subscription: Subscription<M>,
    entity: String,
    concurrency: Int,
    process: suspend (M, MillisInstant) -> Unit,
    beforeDlq: (suspend (M, Exception) -> Unit)? = null,
    batchConfig: (suspend (M) -> BatchConfig?)? = null,
    batchProcess: (suspend (List<M>, List<MillisInstant>) -> Unit)? = null
  ): Job

  /**
   *
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
