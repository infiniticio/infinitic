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
import io.infinitic.common.transport.Topic
import io.infinitic.common.transport.interfaces.TransportConsumer
import io.infinitic.common.transport.interfaces.TransportMessage
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.atomic.AtomicInteger
import kotlin.random.Random

private val logger = KotlinLogging.logger("io.infinitic.tests")

internal var deserializeHook: (IntMessage) -> Unit = { }
internal var acknowledgeHook: (IntMessage) -> Unit = { }
internal var negativeAcknowledgeHook: (IntMessage) -> Unit = { }

private val receivedMutex = Mutex()
internal val receivedList = mutableListOf<Int>()

private val deserializedMutex = Mutex()
internal val deserializedList = mutableListOf<Int>()

private val processedMutex = Mutex()
internal val processedList = mutableListOf<Int>()

internal val acknowledgedMutex = Mutex()
internal val acknowledgedList = mutableListOf<Int>()

private val negativeAcknowledgedMutex = Mutex()
internal val negativeAcknowledgedList = mutableListOf<Int>()

/**
 * Represents an integer message to be transported and processed.
 *
 * This class implements the `TransportMessage` interface with `Int` as the payload type.
 * It encapsulates an integer value and provides mechanisms to serialize, deserialize,
 * acknowledge, and negatively acknowledge the message.
 *
 * @property value The integer value contained in the message.
 * @property messageId The unique identifier for the message, derived from the integer value.
 * @property publishTime The timestamp when the message was published.
 * @property topic The topic to which the message belongs.
 * @property key A key associated with the message, derived from the integer value.
 * @property sentToDeadLetterQueue Indicates whether the message has been sent to the dead-letter queue.
 */
internal data class IntMessage(val value: Int) : TransportMessage<Int> {
  override val messageId: String = value.toString()

  override val publishTime: MillisInstant = MillisInstant.now()

  override lateinit var topic: Topic<*>

  override val key: String = (value % 10).toString()

  override suspend fun deserialize(): Int {
    logger.debug { "start deserializing...$this" }
    deserializeHook(this)
    logger.trace { "end   deserializing...$this" }
    deserializedMutex.withLock { deserializedList.add(value) }
    return value
  }

  override suspend fun negativeAcknowledge() {
    negativeAcknowledgeHook(this)
    negativeAcknowledgedMutex.withLock { negativeAcknowledgedList.add(value) }
  }

  override suspend fun acknowledge() {
    acknowledgeHook(this)
    acknowledgedMutex.withLock { acknowledgedList.add(value) }
  }

  override val sentToDeadLetterQueue = negativeAcknowledgedList.contains(value)

  override fun toString(): String = value.toString()
}

/**
 * A consumer class that processes integer messages by incrementing a counter.
 *
 * This class extends the `TransportConsumer` interface with `IntMessage` as the message type
 * and provides implementations for receiving individual and batch messages. It maintains
 * a counter that increments with each received message.
 *
 * Properties:
 * - counter: An `AtomicInteger` used to keep track of the count of messages.
 * - maxRedeliveryCount: The maximum number of times a message can be redelivered, set to 1.
 * - name: The name of the consumer, defaults to the consumer's `toString()` representation.
 *
 * Methods:
 * - reset: Resets the counter to zero.
 * - receive: Receives an incremented `IntMessage` and adds its value to a received list.
 * - batchReceive: Receives a batch of incremented `IntMessage` objects based on the `batchConfig`.
 */
internal open class IntConsumer(private val maxMessages: Int = 1) : TransportConsumer<IntMessage> {
  private val counter = AtomicInteger(0)

  fun reset() {
    counter.set(0)
  }

  override suspend fun receive() = IntMessage(counter.incrementAndGet())
      .also { receivedMutex.withLock { receivedList.add(it.value) } }

  override suspend fun batchReceive(): List<IntMessage> =
      List(maxMessages) { receive() }

  override val maxRedeliveryCount = 1

  override val name: String = this.toString()
}

internal suspend fun process(deserialized: Int, publishedAt: MillisInstant) {
  logger.debug { "start processing......$deserialized" }
  delay(Random.nextLong(10))
  logger.trace { "end   processing......$deserialized" }
  processedMutex.withLock { processedList.add(deserialized) }
}

internal suspend fun batchProcess(messages: List<Pair<Int, MillisInstant>>) {
  logger.debug { "start processing......${messages.map { it.first }}" }
  delay(Random.nextLong(10))
  logger.trace { "end   processing......${messages.map { it.first }}" }
  processedMutex.withLock { processedList.addAll(messages.map { it.first }) }
}
