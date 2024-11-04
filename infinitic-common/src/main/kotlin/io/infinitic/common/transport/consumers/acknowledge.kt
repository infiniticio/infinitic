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
import io.infinitic.common.transport.interfaces.TransportMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel

/**
 * Collect messages from the channel and acknowledge or handle failures accordingly.
 *
 * @param T The type of the transport message extending `TransportMessage`.
 * @param M The type of the message contained within the transport message.
 * @param I The type of additional information carried by the result.
 * @param beforeDlq A suspending function that will be executed before a message is sent to the dead-letter queue.
 */
context(CoroutineScope, KLogger)
fun <T : TransportMessage<M>, M : Any, I> Channel<Result<T, I>>.acknowledge(
  beforeDlq: (suspend (M, Exception) -> Unit)? = null,
) = collect { result ->
  val message = result.message()

  result.onSuccess {
    message.tryAcknowledge()
  }

  result.onFailure { exception ->
    // negative acknowledge this failed message
    message.tryNegativeAcknowledge()

    // if the message reached a pre-defined number of negative acknowledgment,
    // il will be automatically be sent to Dead-Letter-Queue
    // In that case, we try to run the `beforeDlq` function
    if (message.hasBeenSentToDeadLetterQueue) {
      val deserialized = message.deserializeOrNull()
      // log as error, as this failed message will now be skipped
      error(exception) {
        "Sending message to DLQ: ${deserialized?.string ?: message.messageId}"
      }
      if (deserialized != null && beforeDlq != null) {
        try {
          beforeDlq(deserialized, exception)
        } catch (e: Exception) {
          warn(e) {
            "Error when calling dead letter hook for message ${deserialized.string}"
          }
        }
      }
    }
  }
}

context(KLogger)
private suspend fun <S> TransportMessage<S>.tryAcknowledge() = try {
  trace { "Acknowledging message ${this.messageId}" }
  acknowledge()
  debug { "Acknowledged message ${this.messageId}" }
} catch (e: Exception) {
  warn(e) { "Error when acknowledging ${deserializeOrNull()?.string ?: messageId}" }
}

context(KLogger)
private suspend fun <S> TransportMessage<S>.tryNegativeAcknowledge() = try {
  trace { "Negatively acknowledging message ${this.messageId}" }
  negativeAcknowledge()
  debug { "Negatively acknowledged message ${this.messageId}" }
} catch (e: Exception) {
  warn(e) { "Error when negatively acknowledging ${deserializeOrNull()?.string ?: messageId}" }
  null
}

private fun <S> TransportMessage<S>.deserializeOrNull(): S? = try {
  deserialize()
} catch (e: Exception) {
  null
}
