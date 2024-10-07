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

import io.github.oshai.kotlinlogging.KotlinLogging
import io.infinitic.common.data.MillisInstant
import io.infinitic.common.transport.BatchConfig
import io.infinitic.common.transport.TransportConsumer
import io.infinitic.common.transport.TransportMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.future.await
import kotlinx.coroutines.launch

/**
 * A generic consumer for messages that handles
 * - deserialization,
 * - processing,
 * - acknowledgement and negative acknowledgement.
 *
 * @param S The type of the message implementing the interface [TransportMessage].
 * @param D The type of the deserialized message.
 *
 * @param consumer the transport consumer responsible for receiving messages.
 * @param beforeNegativeAcknowledgement A suspend function called before negatively acknowledging a message.
 */
class ProcessorConsumer<S : TransportMessage, D : Any>(
  val consumer: TransportConsumer<S>,
  val beforeNegativeAcknowledgement: (suspend (Exception, S, D?) -> Unit)?,
) {

  /**
   * Starts an asynchronous operation to process messages received by the consumer.
   *
   * Constraints:
   * - when calling scope is canceled, all ongoing messages should be processed, and the job terminated
   * - an exception is handled
   * - an error triggers a scope cancelling
   *
   * @param concurrency The number of concurrent coroutines to use for processing.
   * @param deserialize A suspending function to deserialize messages.
   * @param process A suspending function to process deserialized messages.
   * @param batchConfig A suspending function to provide batch configuration for deserialized messages, nullable.
   * @param batchProcess A suspending function to process batches of deserialized messages, nullable.
   * @return A Job representing the coroutine for the asynchronous operation.
   */
  fun CoroutineScope.startAsync(
    concurrency: Int,
    deserialize: suspend (S) -> D,
    process: suspend (D, MillisInstant) -> Unit,
    batchConfig: (suspend (D) -> BatchConfig?)? = null,
    batchProcess: (suspend (List<D>, List<MillisInstant>) -> Unit)? = null,
  ): Job = launch {

    with(logger) {
      consumer
          .startConsuming()
          .process(
              concurrency,
              { _, message -> deserialize(message) },
          )
          .batchBy { datum -> batchConfig?.let { it(datum) } }
          .batchProcess(
              concurrency,
              { message, datum -> process(datum, message.publishTime); datum },
              { messages, data -> batchProcess!!(data, messages.map { it.publishTime }); data },
          )
          .collect { result ->
            val message = result.message()
            result.onSuccess {
              acknowledge(message, it)
            }
            result.onFailure {
              val d = try {
                deserialize(message)
              } catch (e: Exception) {
                null
              }
              negativeAcknowledge(it, message, d)
            }
          }
    }
  }

  private suspend fun acknowledge(message: S, deserialize: D) = try {
    consumer.acknowledgeAsync(message).await()
  } catch (e: Exception) {
    logWarn(e) { "Error when acknowledging ${deserialize.string}" }
  }

  private suspend fun negativeAcknowledge(e: Exception, message: S, deserialized: D?) {
    try {
      beforeNegativeAcknowledgement?.let { it(e, message, deserialized) }
    } catch (e: Exception) {
      logWarn(e) {
        "Error when calling negative acknowledgement hook for message " +
            (deserialized?.string ?: message.messageId)
      }
    }
    try {
      consumer.negativeAcknowledgeAsync(message).await()
    } catch (e: Exception) {
      logWarn(e) {
        "Error when negatively acknowledging message " +
            (deserialized?.string ?: message.messageId)
      }
    }
  }

  // No Error should come from logging errors
  private val D.string: String
    get() = try {
      toString()
    } catch (e: Exception) {
      "${this::class.java.name}: Error during toString() - (${e::class.java.name}(${e.message}))"
    }

  private fun logWarn(e: Exception, message: () -> String) = try {
    logger.warn(e, message)
  } catch (e: Exception) {
    System.err.println(message())
  }

  companion object {
    private val logger = KotlinLogging.logger {}
  }
}
