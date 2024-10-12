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
import io.infinitic.common.transport.TransportMessage
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

  debug { "collect: starting listening channel ${this@collect.hashCode()}" }

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

/**
 * Collects and processes results from the channel. Upon successful message processing,
 * the message is acknowledged. If an exception occurs and the negative acknowledgment
 * count reaches the max limit, the message is sent to the dead letter queue (DLQ).
 *
 * @param T Represents a type that extends TransportMessage<M>.
 * @param M Type of the payload contained within the message.
 * @param I Type of additional information passed along with the result.
 * @param maxNegativeAcknowledgement Maximum number of negative acknowledgments
 *        allowed before the message is considered for DLQ.
 * @param beforeDlq A suspending lambda function to be executed before sending
 *        the message to the DLQ. Receives the deserialized message and the exception
 *        that caused the failure as parameters.
 */
context(CoroutineScope, KLogger)
fun <T : TransportMessage<M>, M : Any, I> Channel<Result<T, I>>.collect(
  maxNegativeAcknowledgement: Int? = null,
  beforeDlq: (suspend (M, Exception) -> Unit)? = null,
) = collect { result ->
  val message = result.message()

  result.onSuccess {
    message.tryAcknowledge()
  }

  result.onFailure { exception ->
    val negAckCount = message.tryNegativeAcknowledge()

    if (negAckCount == maxNegativeAcknowledgement) {
      val deserialized = message.deserializeOrNull()
      // log as error as this message will be skipped
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
  acknowledge()
} catch (e: Exception) {
  warn(e) { "Error when acknowledging ${deserializeOrNull()?.string ?: messageId}" }
}

context(KLogger)
private suspend fun <S> TransportMessage<S>.tryNegativeAcknowledge() = try {
  negativeAcknowledge()
} catch (e: Exception) {
  warn(e) { "Error when negatively acknowledging ${deserializeOrNull()?.string ?: messageId}" }
  null
}

private fun <S> TransportMessage<S>.deserializeOrNull(): S? = try {
  deserialize()
} catch (e: Exception) {
  null
}
