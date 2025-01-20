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
package io.infinitic.clients.dispatcher

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull

internal class ResponseFlow<T> {
  private val responseFlow = MutableSharedFlow<Result<T?>>(replay = 1)
      .apply { tryEmit(Result.success(null)) }

  suspend fun emit(value: T) {
    responseFlow.emit(Result.success(value))
  }

  suspend fun emitThrowable(e: Throwable) {
    responseFlow.emit(Result.failure(e))
  }

  suspend fun first(timeout: Long = Long.MAX_VALUE) = first(timeout) { true }

  /**
   * Suspends until the first element that matches the given predicate is emitted by the response flow,
   * or until the timeout is reached, in that case returns null
   */
  suspend fun first(timeout: Long = Long.MAX_VALUE, predicate: suspend (T) -> Boolean): T? =
      withTimeoutOrNull(timeout) {
        var isFirst = true
        responseFlow.first { result ->
          when (isFirst) {
            true -> {
              isFirst = false
              // throw the exception
              result.getOrThrow()
              // ignore the first value if not an exception
              false
            }

            false -> predicate(result.getOrThrow()!!)
          }
        }.getOrThrow()!!
      }
}
