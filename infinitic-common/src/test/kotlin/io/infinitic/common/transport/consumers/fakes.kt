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
import io.infinitic.common.transport.BatchConfig
import io.infinitic.common.transport.Topic
import io.infinitic.common.transport.interfaces.TransportConsumer
import io.infinitic.common.transport.interfaces.TransportMessage
import kotlinx.coroutines.delay
import java.util.*
import java.util.concurrent.atomic.AtomicInteger
import kotlin.random.Random

internal data class IntMessage(val value: Int) : TransportMessage<DeserializedIntMessage> {
  override val messageId: String = value.toString()
  override val publishTime: MillisInstant = MillisInstant.now()

  override lateinit var topic: Topic<*>

  override fun deserialize(): DeserializedIntMessage = DeserializedIntMessage(this)

  override suspend fun negativeAcknowledge() {
    negativeAcknowledgedList.add(value)
  }

  override suspend fun acknowledge() {
    acknowledgedList.add(value)
  }

  override val hasBeenSentToDeadLetterQueue = negativeAcknowledgedList.contains(value)

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

internal open class IntConsumer : TransportConsumer<IntMessage> {
  private val counter = AtomicInteger(0)

  fun reset() {
    counter.set(0)
  }

  override suspend fun receive() = IntMessage(counter.incrementAndGet())
      .also { receivedList.add(it.value) }

  override val maxRedeliveryCount = 1
  override val name: String = this.toString()
}

internal fun deserialize(message: IntMessage) = DeserializedIntMessage(message).also {
  println("start deserializing...$message")
  deserializedList.add(it.value.value)
  println("end   deserializing...$message")
}

internal suspend fun process(deserialized: DeserializedIntMessage, publishTime: MillisInstant) {
  println("start processing......${deserialized.value.value}")
  delay(Random.nextLong(100))
  println("end   processing......${deserialized.value.value}")
  processedList.add(deserialized.value.value)
}

internal fun processBatch(batch: List<DeserializedIntMessage>, publishTimes: List<MillisInstant>) {
  processedList.addAll(batch.map { it.value.value })
}

internal fun batchConfig(deserialized: DeserializedIntMessage): BatchConfig? {
  val i = deserialized.value.value
  return when {
    (i % 3) == 0 -> null
    (i % 3) == 1 -> BatchConfig("1", 20, MillisDuration(1000 * 3600 * 50))
    (i % 3) == 2 -> BatchConfig("2", 20, MillisDuration(1000 * 3600 * 50))
    else -> throw IllegalStateException()
  }
}

