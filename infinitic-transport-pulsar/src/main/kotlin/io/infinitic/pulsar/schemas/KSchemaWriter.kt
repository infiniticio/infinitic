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
package io.infinitic.pulsar.schemas

import io.infinitic.common.messages.Envelope
import io.infinitic.common.messages.toByteArray
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Tag
import io.micrometer.core.instrument.Timer
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong
import org.apache.pulsar.client.api.schema.SchemaWriter

class KSchemaWriter<T : Envelope<*>>(
  workerName: String? = null,
  topic: String? = null,
  registry: MeterRegistry? = null,
) : SchemaWriter<T> {
  private val metrics =
      when {
        registry != null && workerName != null && topic != null ->
          ProducerSerializationMetrics(registry, workerName, topic)
        else -> null
      }

  override fun write(message: T): ByteArray {
    val metrics = metrics ?: return message.toByteArray()
    val messageType = message.message()::class.simpleName ?: "Unknown"
    val inFlight = metrics.inFlight(messageType)
    inFlight.incrementAndGet()
    val startNanos = System.nanoTime()

    try {
      return message.toByteArray()
    } finally {
      metrics.timer(messageType).record(System.nanoTime() - startNanos, TimeUnit.NANOSECONDS)
      inFlight.decrementAndGet()
    }
  }
}

private class ProducerSerializationMetrics(
  private val registry: MeterRegistry,
  private val workerName: String,
  private val topic: String,
) {
  private val inFlight = ConcurrentHashMap<String, AtomicLong>()

  fun inFlight(messageType: String): AtomicLong =
      inFlight.computeIfAbsent(messageType) { mt ->
        AtomicLong(0).also { counter ->
          registry.gauge(
              "infinitic.producer.message.serialization.in_flight",
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

  fun timer(messageType: String): Timer = registry.timer(
      "infinitic.producer.message.serialization",
      "worker_name",
      workerName,
      "topic",
      topic,
      "message_type",
      messageType,
  )
}
