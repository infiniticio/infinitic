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

import io.infinitic.common.data.MillisInstant
import io.infinitic.common.transport.MessageBatchConfig
import io.infinitic.common.transport.TransportConsumer
import io.infinitic.common.transport.TransportMessage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapMerge
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.time.Instant

class ConsumerSharedProcessor<S : TransportMessage, D : Any>(
  private val consumer: TransportConsumer<S>,
  deserialize: suspend (S) -> D,
  process: suspend (D, MillisInstant) -> Unit,
  beforeNegativeAcknowledgement: (suspend (S, D?, Exception) -> Unit)?,
  private val assessBatching: ((D) -> Result<MessageBatchConfig?>)? = null,
  private val processBatch: (suspend (List<D>) -> Unit)? = null
) : AbstractConsumerProcessor<S, D>(
    consumer,
    deserialize,
    process,
    beforeNegativeAcknowledgement,
) {
  init {
    require((processBatch == null) == (assessBatching == null)) {
      "assessBatching and processBatch should be defined together or not at all"
    }
  }

  /**
   * Channel for messages to process
   */
  private val processChannel = Channel<MessageOrMessageList<S, D>>()

  /**
   * A map that holds channels for batching messages.
   * The key is common to all messages batched together
   */
  private val batchingChannels = mutableMapOf<String, Channel<DeserializedMessage<S, D>>>()

  suspend fun start(concurrency: Int) = coroutineScope {
    // Start [concurrency] processors for processChannel
    List(concurrency) { launch { processChannel.startProcessing() } }

    consumer
        .receiveAsFlow()
        .deserialize(concurrency)
        .collect { message ->
          val batchConfigResult = assessBatching?.let { it(message.deserialized) }
          batchConfigResult
              ?.onFailure {
                // assessBatching has the responsibility to tell
                // the parent workflow that this task failed,
                // but the consumer is done with this message
                acknowledge(message)
              }
              ?.onSuccess { batchConfig ->
                when (batchConfig) {
                  // no batch config for this message, we just process it
                  null -> processChannel.send(MessageSingle(message))
                  // we found a batch config
                  else -> batchingChannels.send(batchConfig, message)
                }
              }
          // no assessBatching function, we just process the message
            ?: processChannel.send(MessageSingle(message))
        }
  }

  /**
   * Deserializes messages emitted by a Flow using the specified level of concurrency.
   *
   * @param concurrency The maximum number of concurrent deserialization operations to be performed.
   * @return A Flow emitting deserialized strings.
   */
  @OptIn(ExperimentalCoroutinesApi::class)
  private fun Flow<S>.deserialize(concurrency: Int): Flow<DeserializedMessage<S, D>> =
      flatMapMerge(concurrency) { message ->
        flow { tryDeserialize(message)?.let { emit(it) } }
      }

  /**
   * Starts processing messages from the channel.
   */
  private suspend fun Channel<MessageOrMessageList<S, D>>.startProcessing() {
    for (message in this) {
      withContext(NonCancellable) {
        when (message) {
          is MessageSingle<S, D> -> process(message.deserialized)
          is MessageBatch<S, D> -> processBatch(message.deserialized)
        }
      }
    }
  }

  /**
   * Processes a batch of deserialized messages.
   * (This method should not fail)
   */
  private suspend fun processBatch(messages: List<DeserializedMessage<S, D>>) {
    try {
      processBatch!!(messages.map { it.deserialized })
      acknowledge(messages)
    } catch (e: Exception) {
      logger.error(e) { "error when processing batch ${messages.map { it.transportMessage.messageId }}" }
      negativeAcknowledge(messages, e)
    }
  }

  /**
   * Acknowledges a batch of deserialized message.
   * (This method should not fail)
   */
  private suspend fun acknowledge(messages: List<DeserializedMessage<S, D>>) = try {
    consumer.acknowledge(messages.map { it.transportMessage })
  } catch (e: Exception) {
    logger.warn(e) { "Error when acknowledging batch messages ${messages.map { it.transportMessage.messageId }}" }
  }

  /**
   * Negatively acknowledges a batch of deserialized message.
   * (This method should not fail)
   */
  private suspend fun negativeAcknowledge(
    messages: List<DeserializedMessage<S, D>>,
    cause: Exception
  ) {
    try {
      coroutineScope {
        messages.forEach {
          launch {
            beforeNegativeAcknowledge(it.transportMessage, it.deserialized, cause)
          }
        }
      }
      consumer.negativeAcknowledge(messages.map { it.transportMessage })
    } catch (e: Exception) {
      // the messages should be automatically negatively acknowledged after ackTimeout
      // TODO: check ackTimeout setting
      logger.warn(e) { "Error when negative acknowledging batch messages ${messages.map { it.transportMessage.messageId }}" }
    }
  }

  /**
   * Sends a message to a batched processing channel based on the specified configuration.
   * If needed, creates the batching channel on the fly and start listening to it
   */
  context(CoroutineScope)
  private suspend fun MutableMap<String, Channel<DeserializedMessage<S, D>>>.send(
    config: MessageBatchConfig,
    message: DeserializedMessage<S, D>
  ) {
    getOrPut(config.batchKey) {
      // create channel and start listening to it
      Channel<DeserializedMessage<S, D>>().also { launch { it.startBatchingAndProcessing(config) } }
    }.send(message)
  }

  /**
   * Starts batching and processing  the elements received in the channel
   * using the provided batch configuration.
   */
  private suspend fun Channel<DeserializedMessage<S, D>>.startBatchingAndProcessing(config: MessageBatchConfig) {
    receiveAsFlow()
        .collectBatch(config) { batch ->
          // once a batch completed, send to processChannel as a MessageBatch
          processChannel.send(MessageBatch(batch))
        }
  }

  /**
   * Collects elements from the flow into batches based on the provided configuration and executes
   * a specified action on each batch. The collection is performed within a coroutine scope.
   *
   * @param config The configuration that provides batch size and timeout settings for batching.
   * @param action The action to be performed on each batch of elements.
   */
  internal suspend fun <M> Flow<M>.collectBatch(
    config: MessageBatchConfig,
    action: suspend (List<M>) -> Unit
  ) = coroutineScope {
    require(config.batchSize > 1) { "batch size must be > 1" }

    var nowMillis: Long = 0
    val buffer = mutableListOf<M>()
    val timeoutMillis = config.batchTime.inWholeMilliseconds
    val bufferMutex = Mutex()
    lateinit var timeoutJob: Job

    fun logBatchProcessing(batch: List<M>) {
      val duration = Instant.now().toEpochMilli() - nowMillis
      logger.debug { "Processing ${batch.size} elements for ${config.batchKey} after ${duration}ms" }
    }

    fun CoroutineScope.startTimeoutJob() = launch {
      try {
        delay(timeoutMillis)
        // we reach the timeout, before the batch size
        val batch = bufferMutex.withLock {
          ArrayList(buffer).also { buffer.clear() }
        }
        // just in case
        if (batch.isNotEmpty()) {
          logBatchProcessing(batch)
          action(batch)
        }
      } catch (e: CancellationException) {
        // Do nothing
      }
    }

    try {
      collect { value ->
        var batch: List<M>? = null
        bufferMutex.withLock {
          buffer.add(value)
          when (buffer.size) {
            1 -> {
              // after the first element, we start the timeout job
              nowMillis = Instant.now().toEpochMilli()
              timeoutJob = startTimeoutJob()
            }

            config.batchSize -> {
              // we reach the batch size, before the timeout
              timeoutJob.cancel()
              batch = ArrayList(buffer)
              buffer.clear()
            }

            else -> null
          }
        }
        batch?.let {
          logBatchProcessing(it)
          action(it)
        }
      }
    } catch (e: CancellationException) {
      // Do nothing
    } finally {
      timeoutJob.cancel()
    }
  }

  /**
   * A sealed class representing either a single message or a batch of messages.
   */
  private sealed class MessageOrMessageList<S : TransportMessage, D : Any>

  private data class MessageSingle<S : TransportMessage, D : Any>(val deserialized: DeserializedMessage<S, D>) :
    MessageOrMessageList<S, D>()

  private data class MessageBatch<S : TransportMessage, D : Any>(val deserialized: List<DeserializedMessage<S, D>>) :
    MessageOrMessageList<S, D>()
}

