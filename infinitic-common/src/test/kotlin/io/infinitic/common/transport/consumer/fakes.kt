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

import io.infinitic.common.data.MillisDuration
import io.infinitic.common.data.MillisInstant
import io.infinitic.common.transport.BatchConfig
import io.infinitic.common.transport.TransportConsumer
import io.infinitic.common.transport.TransportMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
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

internal open class IntConsumer : TransportConsumer<IntMessage> {
  private val counter = AtomicInteger(0)

  protected val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

  fun reset() {
    counter.set(0)
  }

  override fun receiveAsync() = scope.future {
    IntMessage(counter.incrementAndGet())
        .also { receivedList.add(it.value) }
  }

  override fun negativeAcknowledgeAsync(messages: List<IntMessage>): CompletableFuture<Unit> =
      scope.future {
        delay(Random.nextLong(5))
            .also { negativeAcknowledgedList.addAll(messages.map { it.value }) }
      }

  override fun acknowledgeAsync(messages: List<IntMessage>): CompletableFuture<Unit> =
      scope.future {
        delay(Random.nextLong(5))
            .also { acknowledgedList.addAll(messages.map { it.value }) }
      }

  override fun negativeAcknowledgeAsync(message: IntMessage): CompletableFuture<Unit> =
      scope.future {
        delay(Random.nextLong(5))
            .also { negativeAcknowledgedList.add(message.value) }
      }

  override fun acknowledgeAsync(message: IntMessage): CompletableFuture<Unit> =
      scope.future {
        delay(Random.nextLong(5))
            .also { acknowledgedList.add(message.value) }
      }
}

internal suspend fun deserialize(value: IntMessage) = DeserializedIntMessage(value).also {
  println("start deserializing...$value")
  delay(Random.nextLong(5))
  deserializedList.add(it.value.value)
  println("end   deserializing...$value")
}


internal suspend fun process(message: DeserializedIntMessage, publishTime: MillisInstant) {
  println("start processing......${message.value.value}")
  delay(Random.nextLong(100))
  println("end   processing......${message.value.value}")
  processedList.add(message.value.value)
}

internal fun processBatch(batch: List<DeserializedIntMessage>, publishTimes: List<MillisInstant>) {
  processedList.addAll(batch.map { it.value.value })
}

internal fun getBatchingConfig(value: DeserializedIntMessage): BatchConfig? {
  val i = value.value.value
  return when {
    (i % 3) == 0 -> null
    (i % 3) == 1 -> BatchConfig("1", 20, MillisDuration(1000 * 3600 * 50))
    (i % 3) == 2 -> BatchConfig("2", 20, MillisDuration(1000 * 3600 * 50))
    else -> throw IllegalStateException()
  }
}

internal fun beforeNegativeAcknowledgement(
  e: Exception,
  message: IntMessage,
  deserialized: DeserializedIntMessage?,
) {
  beforeNegativeAcknowledgedList.add(message.value)
}
