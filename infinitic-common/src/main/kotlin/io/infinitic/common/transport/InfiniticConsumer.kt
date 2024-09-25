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

import io.infinitic.common.data.MillisInstant
import io.infinitic.common.messages.Message
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job

interface InfiniticConsumer {

  /**
   * Starts consuming messages from a given subscription and processes them using the provided handler.
   *
   * The CoroutineScope context is used to start the endless loop that listen for messages
   *
   * @return a job corresponding to the endless loop processing
   *
   * @param S The type of the messages to be consumed.
   * @param subscription The subscription from which to consume messages.
   * @param entity The entity associated with this consumer. (typically a service name or workflow name)
   * @param handler The function to handle each consumed message and its publishing time.
   * @param beforeDlq An optional function to be executed before sending the message to the dead-letter queue (DLQ).
   * @param concurrency The number of concurrent message handlers to be used.
   */
  context(CoroutineScope)
  suspend fun <S : Message> startAsync(
    subscription: Subscription<S>,
    entity: String,
    handler: suspend (S, MillisInstant) -> Unit,
    beforeDlq: (suspend (S?, Exception) -> Unit)?,
    concurrency: Int
  ): Job

  /**
   * Starts consuming messages from a given subscription and processes them using the provided handler.
   *
   * The CoroutineScope context is used to start the endless loop that listen for messages
   *
   * @param S The type of the messages to be consumed.
   * @param subscription The subscription from which to consume messages.
   * @param entity The entity associated with this consumer. (typically a service name or workflow name)
   * @param handler The function to handle each consumed message and its publishing time.
   * @param beforeDlq An optional function to be executed before sending the message to the dead-letter queue (DLQ).
   * @param concurrency The number of concurrent message handlers to be used.
   */
  context(CoroutineScope)
  suspend fun <S : Message> start(
    subscription: Subscription<S>,
    entity: String,
    handler: suspend (S, MillisInstant) -> Unit,
    beforeDlq: (suspend (S?, Exception) -> Unit)?,
    concurrency: Int
  ): Unit = startAsync(subscription, entity, handler, beforeDlq, concurrency).join()
}
