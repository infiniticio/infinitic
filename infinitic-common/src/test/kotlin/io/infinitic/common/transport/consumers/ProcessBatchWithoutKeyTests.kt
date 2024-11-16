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
import io.infinitic.common.transport.config.BatchConfig
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlin.random.Random

internal class ProcessBatchWithoutKeyTests : StringSpec(
    {
      val logger = KotlinLogging.logger("io.infinitic.tests")

      fun getScope() = CoroutineScope(Dispatchers.IO)

      beforeEach {
        deserializeHook = { }
        receivedList.clear()
        deserializedList.clear()
        processedList.clear()
        acknowledgedList.clear()
        negativeAcknowledgedList.clear()
      }

      "Processor stops when current scope is canceled, all ongoing messages should be processed" {
        val maxMessages = 20
        val consumer = IntConsumer(maxMessages)
        val scope = getScope()

        later { scope.cancel() }

        scope.launch {
          coroutineScope {
            startBatchProcessingWithoutKey(
                logger = logger,
                consumer = consumer,
                concurrency = Random.nextInt(5, 10),
                batchConfig = BatchConfig(maxMessages, 3600.0),
                processor = ::batchProcess,
            )
          }
        }.join()

        receivedList.size shouldBeGreaterThan 0
        acknowledgedList.sorted() shouldBe processedList.sorted()
        (acknowledgedList + negativeAcknowledgedList).sorted() shouldBe receivedList.sorted()
      }


      "Batch Processing with custom batch key (maxMessages) should be correct" {
        val maxMessages = 20
        val consumer = IntConsumer(maxMessages)
        val scope = getScope()

        suspend fun batchProcessWithBatchKeyCheck(
          messages: List<Pair<Int, MillisInstant>>
        ) = batchProcess(messages).also {
          val first = messages.first().first % 3
          messages.map { it.first % 3 }.all { it == first } shouldBe true
        }

        later { scope.cancel() }

        scope.launch {
          coroutineScope {
            startBatchProcessingWithoutKey(
                logger = logger,
                consumer = consumer,
                concurrency = Random.nextInt(5, 10),
                batchConfig = BatchConfig(maxMessages, 3600.0),
                getBatchKey = { i -> (i % 3).toString() },
                processor = ::batchProcessWithBatchKeyCheck,
            )
          }
        }.join()

        receivedList.size shouldBeGreaterThan 0
        acknowledgedList.sorted() shouldBe processedList.sorted()
        (acknowledgedList + negativeAcknowledgedList).sorted() shouldBe receivedList.sorted()
      }

      "Batch Processing with custom batch key (MaxSeconds) should be correct" {
        val maxMessages = 20
        val consumer = IntConsumer(maxMessages)
        val scope = getScope()

        suspend fun batchProcessWithBatchKeyCheck(
          messages: List<Pair<Int, MillisInstant>>
        ) = batchProcess(messages).also {
          val first = messages.first().first % 3
          messages.size shouldBeGreaterThan 1
          messages.map { it.first % 3 }.all { it == first } shouldBe true
        }

        later { scope.cancel() }

        scope.launch {
          coroutineScope {
            startBatchProcessingWithoutKey(
                logger = logger,
                consumer = consumer,
                concurrency = Random.nextInt(5, 10),
                batchConfig = BatchConfig(10000, 0.01),
                getBatchKey = { i -> (i % 3).toString() },
                processor = ::batchProcessWithBatchKeyCheck,
            )
          }
        }.join()

        receivedList.size shouldBeGreaterThan 0
        acknowledgedList.sorted() shouldBe processedList.sorted()
        (acknowledgedList + negativeAcknowledgedList).sorted() shouldBe receivedList.sorted()
      }

      "An Error during deserialization triggers quitting, but does not prevent finishing current processing" {
        val maxMessages = 20
        val consumer = IntConsumer(maxMessages)
        deserializeHook = { if (it.value == 1000) throw Error("Expected Error") }

        getScope().launch {
          coroutineScope {
            startBatchProcessingWithoutKey(
                logger = logger,
                consumer = consumer,
                concurrency = Random.nextInt(5, 10),
                batchConfig = BatchConfig(maxMessages, 3600.0),
                processor = ::batchProcess,
            )
          }
        }.join()

        acknowledgedList shouldContainAll (1..940).toList()
        acknowledgedList shouldNotContain 1000
        negativeAcknowledgedList shouldContain 1000
        negativeAcknowledgedList.size % 20 shouldBe 0
        (acknowledgedList + negativeAcknowledgedList).sorted() shouldBe receivedList.sorted()
      }

      "An Error during batch processing triggers quitting, but does not prevent finishing current processing" {
        val maxMessages = 20
        val consumer = IntConsumer(maxMessages)

        suspend fun batchProcessWithError(
          messages: List<Pair<Int, MillisInstant>>
        ) = if (messages.map { it.first }.contains(1000)) throw Error("Expected Error")
        else batchProcess(messages)

        getScope().launch {
          coroutineScope {
            startBatchProcessingWithoutKey(
                logger = logger,
                consumer = consumer,
                concurrency = Random.nextInt(5, 10),
                batchConfig = BatchConfig(maxMessages, 3600.0),
                processor = ::batchProcessWithError,
            )
          }
        }.join()

        acknowledgedList shouldContainAll (1..900).toList()
        acknowledgedList shouldNotContain 1000
        acknowledgedList.sorted() shouldBe processedList.sorted()
        negativeAcknowledgedList.size % 20 shouldBe 0
        (acknowledgedList + negativeAcknowledgedList).sorted() shouldBe receivedList.sorted()
      }

      "An Exception during deserialization triggers negative acknowledgment, but does not finish" {
        val maxMessages = 20
        val consumer = IntConsumer(maxMessages)
        deserializeHook = {
          when (it.value) {
            1000 -> throw Exception("Expected Exception")
            2000 -> throw Error("Expected Error")
          }
        }

        getScope().launch {
          coroutineScope {
            startBatchProcessingWithoutKey(
                logger = logger,
                consumer = consumer,
                concurrency = Random.nextInt(5, 10),
                batchConfig = BatchConfig(maxMessages, 3600.0),
                processor = ::batchProcess,
            )
          }
        }.join()

        acknowledgedList shouldContainAll (1..900).toList()
        acknowledgedList.size shouldBeGreaterThan 1800
        acknowledgedList.sorted() shouldBe processedList.sorted()
        acknowledgedList shouldNotContain 1000
        negativeAcknowledgedList shouldContain 1000
        acknowledgedList.size % maxMessages shouldBe 0
        negativeAcknowledgedList.size % maxMessages shouldBe 0
        (acknowledgedList + negativeAcknowledgedList).sorted() shouldBe receivedList.sorted()
      }

      "An Exception during processing triggers negative acknowledgment, but does not finish" {
        val maxMessages = 20
        val consumer = IntConsumer(maxMessages)

        suspend fun batchProcessWithException(
          messages: List<Pair<Int, MillisInstant>>
        ) = when {
          messages.map { it.first }.contains(1000) -> throw Exception("Expected Exception")
          messages.map { it.first }.contains(2000) -> throw Error("Expected Error")
          else -> batchProcess(messages)
        }

        getScope().launch {
          coroutineScope {
            startBatchProcessingWithoutKey(
                logger = logger,
                consumer = consumer,
                concurrency = 3,
                batchConfig = BatchConfig(maxMessages, 3600.0),
                processor = ::batchProcessWithException,
            )
          }
        }.join()

        acknowledgedList shouldContainAll (1..900).toList()
        acknowledgedList.size shouldBeGreaterThan 1800
        acknowledgedList shouldNotContain 1000
        negativeAcknowledgedList shouldContain 1000
        acknowledgedList.size % maxMessages shouldBe 0
        negativeAcknowledgedList.size % maxMessages shouldBe 0
        (acknowledgedList + negativeAcknowledgedList).sorted() shouldBe receivedList.sorted()
      }
    },
)
