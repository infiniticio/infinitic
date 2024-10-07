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
package io.infinitic.common.transport.consumer

import io.github.oshai.kotlinlogging.KLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Collects results from the channel and processes them using the provided suspending lambda function.
 *
 * @param S The type of the input element.
 * @param process A suspending lambda function to process each received result.
 *                If null, no processing is applied.
 */
context(CoroutineScope, KLogger)
fun <S> Channel<S>.collect(
  process: (suspend (S) -> Unit)? = null
) {
  val callingScope: CoroutineScope = this@CoroutineScope

  launch {
    withContext(NonCancellable) {
      while (true) {
        try {
          // the only way to quit this loop is to close the input channel
          // which is triggered by canceling the calling scope
          val o = receiveIfNotClose().also { trace { "collect: receiving $it" } } ?: break
          process?.invoke(o)
        } catch (e: Error) {
          warn(e) { "Error while collecting, cancelling calling scope" }
          callingScope.cancel()
        }
      }
      trace { "collect: exiting" }
    }
  }
}
