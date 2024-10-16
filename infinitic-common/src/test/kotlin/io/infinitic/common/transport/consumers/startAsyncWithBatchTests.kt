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
import io.infinitic.common.fixtures.later
import io.infinitic.common.transport.BatchConfig
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.collections.shouldNotContainAnyOf
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.ints.shouldBeGreaterThanOrEqual
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel

internal class ProcessorConsumerWithBatchTests : StringSpec(
    {
      val logger = KotlinLogging.logger {}
      val consumer = IntConsumer()

      fun getScope() = CoroutineScope(Dispatchers.IO)

      beforeEach {
        consumer.reset()
        receivedList.clear()
        deserializedList.clear()
        processedList.clear()
        acknowledgedList.clear()
        negativeAcknowledgedList.clear()
      }

      "Processor stops when current scope is canceled, all ongoing messages should be processed" {
        with(logger) {
          val scope = getScope()

          later { scope.cancel() }

          with(scope) {
            consumer
                .startAsync(3, ::deserialize, ::process, null, ::batchConfig, ::processBatch)
                .join()
          }
          receivedList.size shouldBeGreaterThan 0
          acknowledgedList.sorted() shouldBe processedList.sorted()
        }
      }

      "An Error during deserialization triggers quitting, but does not prevent finishing current processing" {
        with(logger) {
          fun deserializeWitError(value: IntMessage) =
              if (value.value == 10) throw Error("Expected Error") else deserialize(value)

          with(getScope()) {
            consumer
                .startAsync(
                    3,
                    ::deserializeWitError,
                    ::process,
                    null,
                    ::batchConfig,
                    ::processBatch,
                )
                .join()
          }

          acknowledgedList shouldContainAll (1..9).toList()
          acknowledgedList shouldNotContain 10
          acknowledgedList.sorted() shouldBe deserializedList.sorted()
          negativeAcknowledgedList shouldBe emptyList()
        }
      }

      "An Error during processing triggers quitting, but does not prevent finishing current processing" {
        with(logger) {
          suspend fun processWithError(
            message: DeserializedIntMessage,
            publishTime: MillisInstant
          ) = if (message.value.value == 12) throw Error("Expected Error")
          else process(message, publishTime)


          with(getScope()) {
            consumer
                .startAsync(
                    3,
                    ::deserialize,
                    ::processWithError,
                    null,
                    ::batchConfig,
                    ::processBatch,
                )
                .join()
          }

          acknowledgedList shouldContainAll (1..11).toList()
          acknowledgedList shouldNotContain 12
          acknowledgedList.sorted() shouldBe processedList.sorted()
          negativeAcknowledgedList shouldBe emptyList()
        }
      }

      "An Error during getBatchingConfig triggers quitting, but does not prevent finishing current processing" {
        with(logger) {
          fun batchConfigWithError(deserialized: DeserializedIntMessage): BatchConfig? =
              if (deserialized.value.value == 10) throw Error("Expected Error")
              else batchConfig(deserialized)

          with(getScope()) {
            consumer
                .startAsync(
                    3,
                    ::deserialize,
                    ::process,
                    null,
                    ::batchConfigWithError,
                    ::processBatch,
                )
                .join()
          }

          acknowledgedList shouldNotContain 10
          acknowledgedList.sorted() shouldBe processedList.sorted()
          negativeAcknowledgedList shouldBe listOf()
        }
      }

      "An Error during batch processing triggers quitting, but does not prevent finishing current processing" {
        with(logger) {
          fun processBatchWithError(
            batch: List<DeserializedIntMessage>, publishTimes: List<MillisInstant>
          ) = if (batch.map { it.value.value }.contains(13)) throw Error("Expected Error")
          else processBatch(batch, publishTimes)

          with(getScope()) {
            consumer
                .startAsync(
                    3,
                    ::deserialize,
                    ::process,
                    null,
                    ::batchConfig,
                    ::processBatchWithError,
                )
                .join()
          }

          acknowledgedList shouldNotContain List(4) { 3 * it + 1 }
          acknowledgedList.sorted() shouldBe processedList.sorted()
          negativeAcknowledgedList shouldBe listOf()
        }
      }

      "An Exception during deserialization triggers negative acknowledgment" {
        with(logger) {
          fun deserializeWithException(value: IntMessage) = when (value.value) {
            10 -> throw Exception("Expected Exception")
            20 -> throw Error("Expected Error")
            else -> deserialize(value)
          }

          with(getScope()) {
            consumer
                .startAsync(
                    3,
                    ::deserializeWithException,
                    ::process,
                    null,
                    ::batchConfig,
                    ::processBatch,
                )
                .join()
          }

          acknowledgedList.size shouldBeGreaterThanOrEqual 18
          deserializedList.sorted() shouldBe acknowledgedList.sorted()
          negativeAcknowledgedList shouldBe listOf(10)
        }
      }

      "An Exception during processing triggers negative acknowledgment" {
        with(logger) {
          suspend fun processWithException(
            message: DeserializedIntMessage,
            publishTime: MillisInstant
          ) = when (message.value.value) {
            12 -> throw Exception("Expected Exception")
            21 -> throw Error("Expected Error")
            else -> process(message, publishTime)
          }

          with(getScope()) {
            consumer
                .startAsync(
                    3,
                    ::deserialize,
                    ::processWithException,
                    null,
                    ::batchConfig,
                    ::processBatch,
                )
                .join()
          }

          processedList.sorted() shouldBe acknowledgedList.sorted()
          negativeAcknowledgedList shouldBe listOf(12)
        }
      }

      "An Exception during getBatchingConfig triggers quitting, but does not prevent finishing current processing" {
        with(logger) {
          fun batchConfigWithException(deserialized: DeserializedIntMessage): BatchConfig? =
              when (deserialized.value.value) {
                10 -> throw Exception("Expected Exception")
                20 -> throw Error("Expected Error")
                else -> batchConfig(deserialized)
              }

          with(getScope()) {
            consumer
                .startAsync(
                    3,
                    ::deserialize,
                    ::process,
                    null,
                    ::batchConfigWithException,
                    ::processBatch,
                )
                .join()
          }

          acknowledgedList shouldContainAll (1..9).toList()
          acknowledgedList shouldNotContain 10
          acknowledgedList.sorted() shouldBe processedList.sorted()
          negativeAcknowledgedList shouldBe listOf(10)
        }
      }

      "An Exception during batch processing triggers quitting, but does not prevent finishing current processing" {
        with(logger) {
          fun processBatchWithException(
            batch: List<DeserializedIntMessage>,
            publishTimes: List<MillisInstant>
          ) = when {
            batch.map { it.value.value }.contains(13) -> throw Exception("Expected Exception")
            batch.map { it.value.value }.contains(61) -> throw Error("Expected Error")
            else -> processBatch(batch, publishTimes)
          }

          with(getScope()) {
            consumer
                .startAsync(
                    3,
                    ::deserialize,
                    ::process,
                    null,
                    ::batchConfig,
                    ::processBatchWithException,
                )
                .join()
          }

          acknowledgedList shouldNotContainAnyOf List(20) { 3 * it + 1 }
          acknowledgedList.sorted() shouldBe processedList.sorted()
          negativeAcknowledgedList shouldContainAll List(20) { 3 * it + 1 }
        }
      }
    },
)
