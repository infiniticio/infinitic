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
import io.infinitic.common.fixtures.runAndCancel
import io.infinitic.common.transport.MessageBatchConfig
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import kotlin.random.Random
import kotlin.time.Duration.Companion.milliseconds

internal class ConsumerBatchedProcessorTests : StringSpec(
    {
      val concurrency = Random.nextInt(2, 100)

      fun noProcess(message: DeserializedIntMessage, publishTime: MillisInstant) {
        throw NoSuchMethodError()
      }

      val processor = ConsumerSharedProcessor(
          Consumer(),
          ::deserialize,
          ::noProcess,
          ::beforeNegativeAcknowledgement,
          ::assessBatching,
          ::processBatch,
      )

      beforeEach {
        receivedList.clear()
        deserializedList.clear()
        processedList.clear()
        acknowledgedList.clear()
        negativeAcknowledgedList.clear()
        beforeNegativeAcknowledgedList.clear()
      }

      "Processor throws CancellationException when current scope is canceled" {
        runAndCancel {
          processor.start(concurrency)
        }
      }

      "On cancellation, all ongoing messages should be processed before closing" {
        runAndCancel {
          processor.start(concurrency)
        }

        checkAllProcessedAreAcknowledged()
        checkBeforeNegativeAcknowledged()
        negativeAcknowledgedList shouldBe emptyList()
      }

      "An Error during deserialization throws" {

        fun deserializeWithError(value: IntMessage) = DeserializedIntMessage(value).also {
          if (it.value.value == 200) throw Error("Expected Error")
          deserializedList.add(it.value.value)
        }

        val processorWithError = ConsumerSharedProcessor(
            Consumer(),
            ::deserializeWithError,
            ::noProcess,
            ::beforeNegativeAcknowledgement,
            ::assessBatching,
            ::processBatch,
        )

        val e = shouldThrow<Error> { processorWithError.start(concurrency) }
        e.message shouldBe "Expected Error"
        checkAllProcessedAreAcknowledged()
        checkBeforeNegativeAcknowledged()
        negativeAcknowledgedList shouldBe emptyList()
      }

      "An Error during batch processing throws" {
        fun processBatchWithError(batch: List<DeserializedIntMessage>) {
          if (batch.map { it.value.value }.contains(200)) throw Error("Expected Error")
          println("processBatch: $batch")
          processedList.addAll(batch.map { it.value.value })
        }

        val processorWithError = ConsumerSharedProcessor(
            Consumer(),
            ::deserialize,
            ::noProcess,
            ::beforeNegativeAcknowledgement,
            ::assessBatching,
            ::processBatchWithError,
        )

        val e = shouldThrow<Error> { processorWithError.start(concurrency) }
        e.message shouldBe "Expected Error"
        checkAllProcessedAreAcknowledged()
        checkBeforeNegativeAcknowledged()
        negativeAcknowledgedList shouldBe emptyList()
      }

      "An exception during deserialization triggers negative acknowledgment" {
        fun deserializeWitError(value: IntMessage) = DeserializedIntMessage(value).also {
          if (it.value.value == 100) throw Exception("Expected Exception")
          if (it.value.value == 400) throw Error("Expected Error")
          deserializedList.add(it.value.value)
        }

        val processorWithException = ConsumerSharedProcessor(
            Consumer(),
            ::deserializeWitError,
            ::noProcess,
            ::beforeNegativeAcknowledgement,
            ::assessBatching,
            ::processBatch,
        )

        shouldThrow<Error> { processorWithException.start(concurrency) }
        checkAllProcessedAreAcknowledged()
        checkBeforeNegativeAcknowledged()
        negativeAcknowledgedList shouldBe listOf(100)
      }

      "An exception during batch processing triggers negative acknowledgment" {

        fun processBatchWithError(batch: List<DeserializedIntMessage>) {
          if (batch.map { it.value.value }.contains(100)) throw Exception("Expected Exception")
          if (batch.map { it.value.value }.contains(200)) throw Error("Expected Error")
          println("processBatch: $batch")
          processedList.addAll(batch.map { it.value.value })
        }

        val processorWithException = ConsumerSharedProcessor(
            Consumer(),
            ::deserialize,
            ::noProcess,
            ::beforeNegativeAcknowledgement,
            ::assessBatching,
            ::processBatchWithError,
        )

        shouldThrow<Error> { processorWithException.start(concurrency) }
        checkAllProcessedAreAcknowledged()
        checkBeforeNegativeAcknowledged()
        negativeAcknowledgedList.contains(100) shouldBe true
        negativeAcknowledgedList.size shouldBe 20
      }

      "Checking batching process by size" {

        fun processBatchWithChecks(batch: List<DeserializedIntMessage>) {
          // checking that batches are correct
          batch.map { it.value.value % 2 }.toSet().size shouldBe 1
          batch.size shouldBe 20

          processedList.addAll(batch.map { it.value.value })
        }

        val processorWithError = ConsumerSharedProcessor(
            Consumer(),
            ::deserialize,
            ::noProcess,
            ::beforeNegativeAcknowledgement,
            ::assessBatching,
            ::processBatchWithChecks,
        )

        runAndCancel {
          processorWithError.start(concurrency)
        }

        checkAllProcessedAreAcknowledged()
        checkBeforeNegativeAcknowledged()
        negativeAcknowledgedList shouldBe emptySet()
      }

      "Checking batching by time" {

        fun assessTimeBatching(value: DeserializedIntMessage): Result<MessageBatchConfig?> {
          val i = value.value.value
          return when {
            i == 0 -> null
            (i % 2) == 0 -> MessageBatchConfig("even", 10.milliseconds, Int.MAX_VALUE)
            (i % 2) == 1 -> MessageBatchConfig("odd", 10.milliseconds, Int.MAX_VALUE)
            else -> throw IllegalStateException()
          }.let { Result.success(it) }
        }

        fun processBatchWithChecks(batch: List<DeserializedIntMessage>) {
          // checking that batches are correct
          batch.map { it.value.value % 2 }.toSet().size shouldBe 1
          println("batch = $batch")
          processedList.addAll(batch.map { it.value.value })
        }

        val processorWithError = ConsumerSharedProcessor(
            Consumer(),
            ::deserialize,
            ::noProcess,
            ::beforeNegativeAcknowledgement,
            ::assessTimeBatching,
            ::processBatchWithChecks,
        )

        runAndCancel {
          processorWithError.start(concurrency)
        }

        checkAllProcessedAreAcknowledged()
        checkBeforeNegativeAcknowledged()
        negativeAcknowledgedList shouldBe emptySet()
      }

    },
)
