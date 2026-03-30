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
import io.infinitic.common.transport.ServiceExecutorTopic
import io.infinitic.common.transport.Topic
import io.infinitic.common.transport.config.BatchConfig
import io.infinitic.common.transport.interfaces.TransportConsumer
import io.infinitic.common.transport.interfaces.TransportMessage
import io.infinitic.common.transport.logged.LoggerWithCounter
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.longs.shouldBeExactly
import io.kotest.matchers.shouldBe
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

internal class TransportMetricsTests : StringSpec(
    {
      val logger = LoggerWithCounter(KotlinLogging.logger("io.infinitic.tests"))

      fun getScope() = CoroutineScope(Dispatchers.IO)

      "startBatching preserves receipt nanos order" {
        with(KotlinLogging.logger {}) {
          val scope = getScope()
          val inputChannel = Channel<Result<String, Int>>()
          val outputChannel = with(scope) { inputChannel.startBatching(2, 1000L) }

          inputChannel.send(Result.success("first", 1, longArrayOf(11L)))
          inputChannel.send(Result.success("second", 2, longArrayOf(22L)))

          val result = outputChannel.receive()
          result.message shouldBe listOf("first", "second")
          result.data shouldBe listOf(1, 2)
          result.receiptNanos.toList() shouldBe listOf(11L, 22L)

          scope.cancel()
        }
      }

      "batchBy keeps each rebatched message paired with its receipt nanos" {
        with(KotlinLogging.logger {}) {
          val scope = getScope()
          val inputChannel = Channel<Result<List<IntMessage>, List<Int>>>()
          val outputChannel = with(scope) {
            inputChannel.batchBy(2, 1000L) { value -> (value % 2).toString() }
          }

          val messages = listOf(IntMessage(1), IntMessage(2), IntMessage(3), IntMessage(4))
          inputChannel.send(
              Result.success(
                  messages,
                  messages.map { it.value },
                  longArrayOf(101L, 102L, 103L, 104L),
              ),
          )

          val results = listOf(outputChannel.receive(), outputChannel.receive())
          val receiptByBatch = results.associate { result ->
            result.data.first() % 2 to result.receiptNanos.toList()
          }

          receiptByBatch[1] shouldBe listOf(101L, 103L)
          receiptByBatch[0] shouldBe listOf(102L, 104L)

          scope.cancel()
        }
      }

      "mixed batch processing records metrics under each runtime message type" {
        val registry = SimpleMeterRegistry()
        val consumer = MixedBatchConsumer()
        val done = CompletableDeferred<Unit>()
        val acknowledged = AtomicInteger(0)
        val scope = getScope()

        val messages = listOf(
            MixedTransportMessage(Alpha(1)) { if (acknowledged.incrementAndGet() == 4) done.complete(Unit) },
            MixedTransportMessage(Beta(2)) { if (acknowledged.incrementAndGet() == 4) done.complete(Unit) },
            MixedTransportMessage(Alpha(3)) { if (acknowledged.incrementAndGet() == 4) done.complete(Unit) },
            MixedTransportMessage(Beta(4)) { if (acknowledged.incrementAndGet() == 4) done.complete(Unit) },
        )

        consumer.messages = messages

        val job = scope.launch {
          startBatchProcessingWithoutKey(
              logger = logger,
              consumer = consumer,
              concurrency = 1,
              batchConfig = BatchConfig(4, 3600.0),
              processor = { },
              registry = registry,
              workerName = "worker-a",
          )
        }

        done.await()
        scope.cancel()
        job.join()

        registry.get("infinitic.consumer.message.handling")
            .tags("worker_name", "worker-a", "topic", "resolved-topic")
            .timer().count() shouldBeExactly 4L
        registry.get("infinitic.consumer.message.deserialization")
            .tags("worker_name", "worker-a", "topic", "resolved-topic")
            .timer().count() shouldBeExactly 4L
        registry.get("infinitic.consumer.message.processing")
            .tags(
                "worker_name",
                "worker-a",
                "topic",
                "resolved-topic",
                "message_type",
                Alpha::class.simpleName!!,
            )
            .timer().count() shouldBeExactly 2L
        registry.get("infinitic.consumer.message.processing")
            .tags(
                "worker_name",
                "worker-a",
                "topic",
                "resolved-topic",
                "message_type",
                Beta::class.simpleName!!,
            )
            .timer().count() shouldBeExactly 2L
      }
    },
)

private sealed interface MixedPayload

private data class Alpha(val id: Int) : MixedPayload

private data class Beta(val id: Int) : MixedPayload

private class MixedTransportMessage(
  private val payload: MixedPayload,
  private val onAck: () -> Unit,
) : TransportMessage<MixedPayload> {
  override val publishTime: MillisInstant = MillisInstant.now()
  override val messageId: String = payload.toString()
  override val topic: Topic<*> = ServiceExecutorTopic
  override val key: String? = null

  override suspend fun deserialize(): MixedPayload = payload

  override suspend fun acknowledge() {
    onAck()
  }

  override suspend fun negativeAcknowledge() = Unit

  override val sentToDeadLetterQueue: Boolean = false
}

private class MixedBatchConsumer : TransportConsumer<MixedTransportMessage> {
  private val emitted = AtomicBoolean(false)
  lateinit var messages: List<MixedTransportMessage>

  override suspend fun receive(): MixedTransportMessage = error("unused")

  override suspend fun batchReceive(): List<MixedTransportMessage> =
      if (emitted.compareAndSet(false, true)) messages else {
        while (true) {
          delay(1000)
        }
        emptyList()
      }

  override val maxRedeliveryCount: Int = 1
  override val name: String = "mixed-consumer"
  override val topic: String = "resolved-topic"
}
