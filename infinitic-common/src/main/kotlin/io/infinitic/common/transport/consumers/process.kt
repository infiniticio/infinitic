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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Processes elements from the receiving channel by applying a processing function to each element.
 *
 * If the input channel is closed, all ongoing results are sent,
 * then the output channel is closed
 *
 * @param I The type of input elements.
 * @param O The type of output elements.
 * @param process A suspending function that takes an input element and produces an output element.
 * @return A new channel that contains the processed results.
 */
context(CoroutineScope, KLogger)
fun <T : Any, I, O> Channel<Result<T, I>>.process(
  to: Channel<Result<T, O>> = Channel(),
  process: suspend (T, I) -> O,
): Channel<Result<T, O>> {
  val callingScope = this@CoroutineScope

  debug { "process: starting listening channel ${this@process.hashCode()}" }

  launch {
    // start a non cancellable scope
    withContext(NonCancellable) {
      to.addProducer("process")
      while (true) {
        try {
          // the only way to quit this loop is to close the input channel
          // which is triggered by canceling the calling scope
          val result = receiveIfNotClose().also { trace { "process: receiving $it " } } ?: break
          result.onSuccess {
            try {
              val o = process(result.message, it)
              to.send(result.success(o))
            } catch (e: Exception) {
              to.send(result.failure(e))
              warn(e) { "Exception while processing" }
            } catch (e: Error) {
              to.send(result.failure(RuntimeException(e)))
              throw e
            }
          }
          result.onFailure {
            to.send(result.failure(it))
          }
        } catch (e: Throwable) {
          warn(e) { "Error while processing, cancelling calling scope" }
          callingScope.cancel()
        }
      }
      to.removeProducer("process")
    }
  }

  return to
}
