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
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import kotlin.random.Random

internal class ConsumerSharedProcessorTests : StringSpec(
    {
      val concurrency = Random.nextInt(100)

      val processor = ConsumerSharedProcessor(
          Consumer(),
          ::deserialize,
          ::process,
          ::beforeNegativeAcknowledgement,
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
        fun deserializeWitError(value: IntMessage) = DeserializedIntMessage(value).also {
          if (it.value.value == 200) throw Error("Expected Error")
          deserializedList.add(it.value.value)
        }

        val processorWithError = ConsumerSharedProcessor(
            Consumer(),
            ::deserializeWitError,
            ::process,
            ::beforeNegativeAcknowledgement,
        )

        val e = shouldThrow<Error> { processorWithError.start(concurrency) }
        e.message shouldBe "Expected Error"
        checkAllProcessedAreAcknowledged()
        checkBeforeNegativeAcknowledged()
        negativeAcknowledgedList shouldBe emptyList()
      }

      "An Error during processing throws" {
        fun processWithError(message: DeserializedIntMessage, publishTime: MillisInstant) {
          if (message.value.value == 200) throw Error("Expected Error")
          processedList.add(message.value.value)
        }

        val processorWithError = ConsumerSharedProcessor(
            Consumer(),
            ::deserialize,
            ::processWithError,
            ::beforeNegativeAcknowledgement,
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
          if (it.value.value == 200) throw Error("Expected Error")
          deserializedList.add(it.value.value)
        }

        val processorWithException = ConsumerSharedProcessor(
            Consumer(),
            ::deserializeWitError,
            ::process,
            ::beforeNegativeAcknowledgement,
        )

        shouldThrow<Error> { processorWithException.start(concurrency) }
        checkAllProcessedAreAcknowledged()
        checkBeforeNegativeAcknowledged()
        negativeAcknowledgedList shouldBe listOf(100)
      }

      "An exception during processing triggers negative acknowledgment" {
        fun processWithException(message: DeserializedIntMessage, publishTime: MillisInstant) {
          if (message.value.value == 100) throw Exception("Expected Exception")
          if (message.value.value == 200) throw Error("Expected Error")
          processedList.add(message.value.value)
        }

        val processorWithException = ConsumerSharedProcessor(
            Consumer(),
            ::deserialize,
            ::processWithException,
            ::beforeNegativeAcknowledgement,
        )

        shouldThrow<Error> { processorWithException.start(concurrency) }
        checkAllProcessedAreAcknowledged()
        checkBeforeNegativeAcknowledged()
        negativeAcknowledgedList shouldBe listOf(100)
      }
    },
)
