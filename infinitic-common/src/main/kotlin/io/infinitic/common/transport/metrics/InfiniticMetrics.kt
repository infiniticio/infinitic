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
package io.infinitic.common.transport.metrics

import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Tag
import io.micrometer.core.instrument.Timer
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong

class InfiniticMetrics(
  val registry: MeterRegistry,
  val workerName: String,
  val topic: String,
) {
  val executionTimer: Timer = registry.timer(
      "infinitic.consumer.message.handling",
      "worker_name",
      workerName,
      "topic",
      topic,
  )

  val deserializationTimer: Timer = registry.timer(
      "infinitic.consumer.message.deserialization",
      "worker_name",
      workerName,
      "topic",
      topic,
  )

  private val _deserializationInFlight = AtomicLong(0).also { counter ->
    registry.gauge(
        "infinitic.consumer.message.deserialization.in_flight",
        listOf(Tag.of("worker_name", workerName), Tag.of("topic", topic)),
        counter,
        AtomicLong::toDouble,
    )
  }

  val deserializationInFlight: AtomicLong
    get() = _deserializationInFlight

  private val processingInFlight = ConcurrentHashMap<String, AtomicLong>()

  fun processingInFlight(messageType: String): AtomicLong =
      processingInFlight.computeIfAbsent(messageType) { mt ->
        AtomicLong(0).also { counter ->
          registry.gauge(
              "infinitic.consumer.message.processing.in_flight",
              listOf(
                  Tag.of("worker_name", workerName),
                  Tag.of("topic", topic),
                  Tag.of("message_type", mt),
              ),
              counter,
              AtomicLong::toDouble,
          )
        }
      }

  fun processingTimer(messageType: String): Timer = registry.timer(
      "infinitic.consumer.message.processing",
      "worker_name",
      workerName,
      "topic",
      topic,
      "message_type",
      messageType,
  )
}

internal fun messageTypeOf(message: Any?): String = message?.let {
  it::class.simpleName ?: "Unknown"
} ?: "Unknown"

internal suspend fun <T> InfiniticMetrics?.recordDeserialization(block: suspend () -> T): T {
  if (this == null) return block()

  deserializationInFlight.incrementAndGet()
  val startNanos = System.nanoTime()

  try {
    return block()
  } finally {
    deserializationTimer.record(System.nanoTime() - startNanos, TimeUnit.NANOSECONDS)
    deserializationInFlight.decrementAndGet()
  }
}

internal suspend fun InfiniticMetrics?.recordProcessing(
  messageType: String,
  block: suspend () -> Unit,
) {
  if (this == null) {
    block()
    return
  }

  val inFlight = processingInFlight(messageType)
  inFlight.incrementAndGet()
  val startNanos = System.nanoTime()

  try {
    block()
  } finally {
    processingTimer(messageType).record(System.nanoTime() - startNanos, TimeUnit.NANOSECONDS)
    inFlight.decrementAndGet()
  }
}

internal suspend fun InfiniticMetrics?.recordBatchProcessing(
  messages: List<*>,
  block: suspend () -> Unit,
) {
  if (this == null) {
    block()
    return
  }

  val typeCounts = messages.groupingBy(::messageTypeOf).eachCount()
  val inFlightByType = typeCounts.mapValues { processingInFlight(it.key) }

  inFlightByType.forEach { (messageType, counter) ->
    counter.addAndGet(typeCounts.getValue(messageType).toLong())
  }

  val startNanos = System.nanoTime()

  try {
    block()
  } finally {
    val duration = System.nanoTime() - startNanos

    typeCounts.forEach { (messageType, count) ->
      val timer = processingTimer(messageType)
      repeat(count) {
        timer.record(duration, TimeUnit.NANOSECONDS)
      }
      inFlightByType.getValue(messageType).addAndGet(-count.toLong())
    }
  }
}
