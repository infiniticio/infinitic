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
import io.infinitic.common.transport.logged.LoggerWithCounter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

/**
 * Collect messages from the channel and acknowledge or handle failures accordingly.
 *
 * @param T The type of the transport message extending `TransportMessage`.
 * @param M The type of the message contained within the transport message.
 * @param beforeDlq A suspending function that will be executed before a message is sent to the dead-letter queue.
 */
context(CoroutineScope, LoggerWithCounter)
fun <T : TransportMessage<M>, M> Channel<Result<T, Unit>>.acknowledge(
  beforeDlq: (suspend (M, Exception) -> Unit)? = null,
) = collect { result ->
  val message = result.message

  result.onSuccess {
    // acknowledge this successful message
    message.tryAcknowledge()
  }

  result.onFailure { exception ->
    // negative acknowledge this failed message
    message.tryNegativeAcknowledge()
    // dead letter queue hook
    message.dlq(exception, beforeDlq)
  }

  // counter
  decr()
}

/**
 * Collects results from the channel and acknowledge successful messages
 * or negative acknowledge failing ones
 *
 * @param T The type of the transport message.
 * @param M The type of the payload contained within the message.
 * @param beforeDlq A suspending function called before sending the message to the DLQ.
 *                  This function takes a message and an exception as parameters. It is optional and
 *                  defaults to null.
 */
context(CoroutineScope, LoggerWithCounter)
fun <T : TransportMessage<M>, M> Channel<Result<List<T>, Unit>>.acknowledge(
  beforeDlq: (suspend (M, Exception) -> Unit)? = null,
) {
  collect { result ->
    val messages = result.message

    result.onSuccess {
      // acknowledge these successful messages
      coroutineScope {
        messages.forEach {
          launch {
            it.tryAcknowledge()
          }
        }
      }
    }

    result.onFailure { exception ->
      // negative acknowledge these failed messages
      coroutineScope {
        messages.forEach {
          launch {
            // negative acknowledge this failed message
            it.tryNegativeAcknowledge()
            // dead letter queue hook
            it.dlq(exception, beforeDlq)
          }
        }
      }
    }

    // counter
    decr(messages.size)
  }
}

/**
 * if the message reached a pre-defined number of negative acknowledgment,
 * il will be automatically be sent to Dead-Letter-Queue
 * In that case, we try to run the `beforeDlq` function
 */
context(KLogger)
private suspend fun <T : TransportMessage<M>, M> T.dlq(
  e: Exception,
  beforeDlq: (suspend (M, Exception) -> Unit)?
) {
  if (sentToDeadLetterQueue) {
    val deserialized = deserializeOrNull()
    // log as error, as this failed message will now be skipped
    error(e) {
      "Sending message to DLQ: ${deserialized?.string ?: messageId}"
    }
    if (deserialized != null && beforeDlq != null) {
      try {
        beforeDlq(deserialized, e)
      } catch (e: Exception) {
        warn(e) {
          "Error when calling dead letter hook for message ${deserialized.string}"
        }
      }
    }
  }
}

context(KLogger)
private suspend fun <T : TransportMessage<M>, M> T.tryAcknowledge() = try {
  trace { "Acknowledging message $messageId" }
  acknowledge()
  debug { "Acknowledged message $messageId" }
} catch (e: Exception) {
  warn(e) { "Error when acknowledging $messageId" }
}

context(KLogger)
internal suspend fun <T : TransportMessage<M>, M> T.tryNegativeAcknowledge() = try {
  trace { "Negatively acknowledging message $messageId" }
  negativeAcknowledge()
  debug { "Negatively acknowledged message $messageId" }
} catch (e: Exception) {
  warn(e) { "Error when negatively acknowledging $messageId" }
  null
}

private suspend fun <T : TransportMessage<M>, M> T.deserializeOrNull(): M? = try {
  deserialize()
} catch (_: Exception) {
  null
}
