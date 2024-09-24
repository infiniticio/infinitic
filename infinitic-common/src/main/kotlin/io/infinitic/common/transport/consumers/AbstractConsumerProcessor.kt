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

import io.github.oshai.kotlinlogging.KotlinLogging
import io.infinitic.common.data.MillisInstant
import io.infinitic.common.transport.TransportConsumer
import io.infinitic.common.transport.TransportMessage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.future.await
import kotlinx.coroutines.isActive

abstract class AbstractConsumerProcessor<S : TransportMessage, D : Any>(
  private val consumer: TransportConsumer<S>,
  private val deserialize: suspend (S) -> D,
  private val process: suspend (D, MillisInstant) -> Unit,
  private val beforeNegativeAcknowledgement: (suspend (S, D?, Exception) -> Unit)?
) {
  protected val logger = KotlinLogging.logger {}

  /**
   * Converts the asynchronous message receiving mechanism of a Consumer into a Flow of strings.
   *
   * The function continuously receives messages from the Consumer using its `receiveAsync` method,
   * emits them to the Flow, and handles any potential exceptions. The loop breaks if a
   * `CancellationException` is encountered, ensuring graceful shutdown of the flow.
   */
  protected fun TransportConsumer<S>.receiveAsFlow(): Flow<S> = flow {
    while (currentCoroutineContext().isActive) {
      try {
        emit(receiveAsync().await())
      } catch (e: CancellationException) {
        break
      } catch (e: Exception) {
        logger.error(e) { "Error when receiving message from consumer" }
      }
    }
  }

  /**
   * Attempts to deserialize a given transport message.
   * In case of an error, it logs the error, sends a negative acknowledgment, and returns null.
   */
  protected suspend fun tryDeserialize(message: S): DeserializedMessage<S, D>? = try {
    DeserializedMessage(
        transportMessage = message,
        deserialized = deserialize(message),
    )
  } catch (e: Exception) {
    logger.error(e) { "Error when deserializing message ${message.string}" }
    negativeAcknowledge(message, null, e)
    null
  }

  /**
   * Processes a deserialized message.
   */
  protected suspend fun process(message: DeserializedMessage<S, D>) {
    try {
      process(message.deserialized, message.transportMessage.publishTime)
      acknowledge(message)
    } catch (e: Exception) {
      logger.error(e) { "error when processing message ${message.deserialized.string}" }
      negativeAcknowledge(message.transportMessage, message.deserialized, e)
    }
  }

  /**
   * Acknowledges the deserialized message.
   */
  private suspend fun acknowledge(message: DeserializedMessage<S, D>) = try {
    consumer.acknowledge(message.transportMessage)
  } catch (e: Exception) {
    logger.warn(e) { "Error when acknowledging message ${message.string}" }
  }

  /**
   * Invoked before sending a negative acknowledgment for a message.
   *
   * This method serves as a pre-processing step prior to issuing a negative acknowledgment
   * for a given transport message. It logs the message processing attempt, executes any
   * registered `beforeNegativeAcknowledgement` callback, and catches any exceptions that
   * occur during this step.
   */
  protected suspend fun beforeNegativeAcknowledge(
    transportMessage: S,
    deserialized: D?,
    cause: Exception
  ) {
    val msg by lazy { "beforeNegativeAcknowledgement for ${deserialized?.string ?: transportMessage.messageId}}" }
    try {
      beforeNegativeAcknowledgement?.let {
        logger.debug { "Processing $msg" }
        it(
            transportMessage,
            deserialized,
            cause,
        )
        logger.trace { "Processed $msg" }
      }
    } catch (e: Exception) {
      logger.warn(e) { "Exception when processing $msg" }
    }
  }

  /**
   * Sends a negative acknowledgment for a transport message.
   * This method should not fail
   */
  private suspend fun negativeAcknowledge(
    transportMessage: S,
    deserialized: D?,
    cause: Exception
  ) {
    try {
      beforeNegativeAcknowledge(transportMessage, deserialized, cause)
      consumer.negativeAcknowledge(transportMessage)
    } catch (e: Exception) {
      // the message should be automatically negatively acknowledged after ackTimeout
      // TODO: check ackTimeout setting
      logger.warn(e) { "Error when negative acknowledging message ${deserialized?.string ?: transportMessage.messageId}" }
    }
  }

  /**
   * A data class that holds the original transport message alongside its deserialized content.
   */
  protected data class DeserializedMessage<S : TransportMessage, D : Any>(
    val transportMessage: S,
    val deserialized: D,
  ) {
    // We ensure this never fails, as it is used in catch structure
    override fun toString(): String = "DeserializedMessage(" +
        "transportMessageId=${transportMessage.messageId}, " +
        "deserialized=${deserialized.string}" +
        ")"
  }
}





