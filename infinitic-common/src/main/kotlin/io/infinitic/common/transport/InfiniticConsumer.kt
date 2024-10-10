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

import io.github.oshai.kotlinlogging.KLogger
import io.infinitic.common.data.MillisInstant
import io.infinitic.common.messages.Message
import io.infinitic.common.transport.consumers.ProcessorConsumer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

interface InfiniticConsumer {

  /**
   * Builds a list of transport consumers for a given subscription and entity.
   *
   * @param M The type of the messages to be consumed.
   * @param subscription The subscription from which to consume messages.
   * @param entity The entity associated with this consumer.
   * @param occurrence Optional parameter to specify the number of consumers to build.
   * @return A list of transport consumers for the specified subscription and entity.
   */
  suspend fun <M : Message> buildConsumers(
    subscription: Subscription<M>,
    entity: String,
    occurrence: Int? = null
  ): List<TransportConsumer<out TransportMessage<M>>>

  /**
   * Builds a single transport consumer for a given subscription and entity.
   *
   * @param M The type of the messages to be consumed.
   * @param subscription The subscription from which to consume messages.
   * @param entity The entity associated with this consumer.
   * @return A transport consumer for the specified subscription and entity.
   */
  suspend fun <M : Message> buildConsumer(
    subscription: Subscription<M>,
    entity: String,
  ): TransportConsumer<out TransportMessage<M>> = buildConsumers(subscription, entity).first()

  /**
   * Starts consuming messages from a given subscription and processes them using the provided handler.
   *
   * The CoroutineScope context is used to start the endless loop that listen for messages
   *
   * @return a job corresponding to the endless loop processing
   *
   * @param M The type of the messages to be consumed.
   * @param subscription The subscription from which to consume messages.
   * @param entity The entity associated with this consumer. (typically a service name or workflow name)
   * @param process The function to handle each consumed message and its publishing time.
   * @param beforeDlq An optional function to be executed before sending the message to the dead-letter queue (DLQ).
   * @param concurrency The number of concurrent message handlers to be used.
   */
  context(CoroutineScope)
  suspend fun <M : Message> startAsync(
    subscription: Subscription<M>,
    entity: String,
    concurrency: Int,
    process: suspend (M, MillisInstant) -> Unit,
    beforeDlq: (suspend (M?, Exception) -> Unit)?,
    batchConfig: (suspend (M) -> BatchConfig?)? = null,
    batchProcess: (suspend (List<M>, List<MillisInstant>) -> Unit)? = null
  ): Job

  /**
   * Starts consuming messages from a given subscription and processes them using the provided handler.
   *
   * The CoroutineScope context is used to start the endless loop that listen for messages
   *
   * @param M The type of the messages to be consumed.
   * @param subscription The subscription from which to consume messages.
   * @param entity The entity associated with this consumer. (typically a service name or workflow name)
   * @param process The function to handle each consumed message and its publishing time.
   * @param beforeDlq An optional function to be executed before sending the message to the dead-letter queue (DLQ).
   * @param concurrency The number of concurrent message handlers to be used.
   */
  context(CoroutineScope)
  suspend fun <M : Message> start(
    subscription: Subscription<M>,
    entity: String,
    concurrency: Int,
    process: suspend (M, MillisInstant) -> Unit,
    beforeDlq: (suspend (M?, Exception) -> Unit)?,
    batchConfig: (suspend (M) -> BatchConfig?)? = null,
    batchProcess: (suspend (List<M>, List<MillisInstant>) -> Unit)? = null
  ): Unit = startAsync(
      subscription,
      entity,
      concurrency,
      process,
      beforeDlq,
      batchConfig,
      batchProcess,
  ).join()

  context(CoroutineScope, KLogger)
  suspend fun <M : Message> startAsync(
    subscription: Subscription<M>,
    entity: String,
    concurrency: Int,
    maxRedeliverCount: Int,
    process: suspend (M, MillisInstant) -> Unit,
    beforeDlq: (suspend (M?, Exception) -> Unit)?,
    batchConfig: (suspend (M) -> BatchConfig?)?,
    batchProcess: (suspend (List<M>, List<MillisInstant>) -> Unit)?
  ): Job {

    val loggedDeserialize: suspend (TransportMessage<M>) -> M = { message ->
      debug { "Deserializing message: ${message.messageId}" }
      message.deserialize().also {
        trace { "Deserialized message: ${message.messageId}" }
      }
    }

    val loggedHandler: suspend (M, MillisInstant) -> Unit = { message, publishTime ->
      debug { "Processing $message" }
      process(message, publishTime)
      trace { "Processed $message" }
    }

    val beforeNegativeAcknowledgement: suspend (TransportMessage<M>, M?, Exception) -> Unit =
        { message, deserialized, cause ->
          if (message.redeliveryCount == maxRedeliverCount) {
            beforeDlq?.let {
              debug { "Processing beforeNegativeAcknowledgement for ${deserialized ?: message.messageId}" }
              it(deserialized, cause)
              trace { "Processed beforeNegativeAcknowledgement for ${deserialized ?: message.messageId}" }
            }
          }
        }

    return when (subscription.withKey) {
      true -> {
        // build the consumers synchronously (but in parallel)
        val consumers: List<TransportConsumer<out TransportMessage<M>>> =
            buildConsumers(subscription, entity, concurrency)
        launch {
          List(concurrency) { index ->
            val processor = ProcessorConsumer(consumers[index], beforeNegativeAcknowledgement)
            with(processor) { startAsync(1, loggedDeserialize, loggedHandler) }
          }
        }
      }

      false -> {
        // build the unique consumer synchronously
        val consumer = buildConsumer(subscription, entity)
        val processor = ProcessorConsumer(consumer, beforeNegativeAcknowledgement)
        with(processor) {
          startAsync(
              concurrency,
              loggedDeserialize,
              loggedHandler,
              batchConfig,
              batchProcess,
          )
        }
      }
    }
  }
}
