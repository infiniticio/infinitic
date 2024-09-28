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

import io.infinitic.common.data.MillisDuration
import io.infinitic.common.data.MillisInstant
import io.infinitic.common.transport.MessageBatchConfig
import io.infinitic.common.transport.TransportConsumer
import io.infinitic.common.transport.TransportMessage
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.future.future
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.atomic.AtomicInteger
import kotlin.random.Random

internal data class IntMessage(val value: Int) : TransportMessage {
  override val messageId: String = value.toString()
  override val redeliveryCount: Int = Random.nextInt(3)
  override val publishTime: MillisInstant = MillisInstant.now()
  override fun toString(): String = value.toString()
}

internal data class DeserializedIntMessage(val value: IntMessage) {
  override fun toString(): String = value.toString()
}

internal val receivedList = Collections.synchronizedList(mutableListOf<Int>())
internal val deserializedList = Collections.synchronizedList(mutableListOf<Int>())
internal val processedList = Collections.synchronizedList(mutableListOf<Int>())
internal val acknowledgedList = Collections.synchronizedList(mutableListOf<Int>())
internal val negativeAcknowledgedList = Collections.synchronizedList(mutableListOf<Int>())
internal val beforeNegativeAcknowledgedList = Collections.synchronizedList(mutableListOf<Int>())

fun checkAllProcessedAreAcknowledged() {
  // no double in processed
  processedList.toSet().size shouldBe processedList.size
  // all processed are acknowledged
  processedList.sorted() shouldBe acknowledgedList.sorted()
}

fun checkBeforeNegativeAcknowledged() {
  // no double negative acknowledging
  negativeAcknowledgedList.toSet().size shouldBe negativeAcknowledgedList.size
  // all negatively acknowledged have processed beforeNegativeAcknowledge
  negativeAcknowledgedList.sorted() shouldBe beforeNegativeAcknowledgedList.sorted()
}

internal class Consumer : TransportConsumer<IntMessage> {
  val counter = AtomicInteger(0)

  private val scope = CoroutineScope(Dispatchers.IO)

  override fun receiveAsync() = scope.future {
    IntMessage(counter.incrementAndGet())
  }.thenApply { it.also { receivedList.add(it.value) } }

  override fun negativeAcknowledgeAsync(messages: List<IntMessage>): CompletableFuture<Unit> =
      scope.future { delay(Random.nextLong(5)) }
          .thenApply { negativeAcknowledgedList.addAll(messages.map { it.value }) }

  override fun acknowledgeAsync(messages: List<IntMessage>): CompletableFuture<Unit> =
      scope.future { delay(Random.nextLong(5)) }
          .thenApply { acknowledgedList.addAll(messages.map { it.value }) }

  override fun negativeAcknowledgeAsync(message: IntMessage): CompletableFuture<Unit> =
      scope.future { delay(Random.nextLong(5)) }
          .thenApply { negativeAcknowledgedList.add(message.value) }

  override fun acknowledgeAsync(message: IntMessage): CompletableFuture<Unit> =
      scope.future { delay(Random.nextLong(5)) }
          .thenApply { acknowledgedList.add(message.value) }
}

internal fun deserialize(value: IntMessage) = DeserializedIntMessage(value).also {
  deserializedList.add(it.value.value)
}


internal fun process(message: DeserializedIntMessage, publishTime: MillisInstant) {
  processedList.add(message.value.value)
}

internal fun processBatch(batch: List<DeserializedIntMessage>) {
  processedList.addAll(batch.map { it.value.value })
}

internal fun assessBatching(value: DeserializedIntMessage): Result<MessageBatchConfig?> {
  val i = value.value.value
  return when {
    i == 0 -> null
    (i % 2) == 0 -> MessageBatchConfig("even", MillisDuration(1000 * 3600 * 50), 20)
    (i % 2) == 1 -> MessageBatchConfig("odd", MillisDuration(1000 * 3600 * 50), 20)
    else -> throw IllegalStateException()
  }.let { Result.success(it) }
}

internal fun beforeNegativeAcknowledgement(
  message: IntMessage,
  value: DeserializedIntMessage?,
  e: Exception
) {
  beforeNegativeAcknowledgedList.add(message.value)
}
