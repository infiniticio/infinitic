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

import kotlinx.coroutines.future.await
import java.util.concurrent.CompletableFuture

interface TransportConsumer<T : TransportMessage> {
  fun receiveAsync(): CompletableFuture<T>

  fun acknowledgeAsync(message: T): CompletableFuture<Unit>
  fun negativeAcknowledgeAsync(message: T): CompletableFuture<Unit>

  fun acknowledgeAsync(messages: List<T>): CompletableFuture<Unit>
  fun negativeAcknowledgeAsync(messages: List<T>): CompletableFuture<Unit>

  suspend fun acknowledge(message: T): Unit = acknowledgeAsync(message).await()
  suspend fun negativeAcknowledge(message: T): Unit = negativeAcknowledgeAsync(message).await()

  suspend fun acknowledge(messages: List<T>): Unit = acknowledgeAsync(messages).await()
  suspend fun negativeAcknowledge(messages: List<T>): Unit =
      negativeAcknowledgeAsync(messages).await()
}
