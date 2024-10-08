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
import io.infinitic.common.transport.TransportConsumer
import io.infinitic.common.transport.TransportMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException

/**
 * Starts consuming messages from the consumer in the given coroutine scope.
 * Consumed messages are sent through the returned channel as a Result containing
 * both the original message and the resulting message.
 *
 * @return A channel that emits Result objects containing the original and resulting messages.
 */
context(CoroutineScope, KLogger)
internal fun <S : TransportMessage> TransportConsumer<S>.startConsuming(): Channel<Result<S, S>> {
  val channel = Channel<Result<S, S>>()
  val scope = this@CoroutineScope

  scope.launch {
    while (isActive) {
      try {
        val msg = receive().also { trace { "consuming: received $it" } }
        channel.send(Result.success(msg, msg))
      } catch (e: CancellationException) {
        // do nothing, will exit if calling scope is not active anymore
      } catch (e: Exception) {
        warn(e) { "Exception when receiving message from $this" }
      } catch (e: Error) {
        warn(e) { "Error when receiving message from $this" }
        // canceling current scope
        scope.cancel()
      }
    }
    debug { "consuming: exiting" }
    channel.close()
  }

  return channel
}
