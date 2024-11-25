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
import io.infinitic.common.transport.logged.LoggerWithCounter
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.ints.shouldBeGreaterThanOrEqual
import io.kotest.matchers.ints.shouldBeLessThanOrEqual
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlin.random.Random

internal class ProcessWithoutKeyTests : StringSpec(
    {
      val logger = LoggerWithCounter(KotlinLogging.logger("io.infinitic.tests"))

      fun getScope() = CoroutineScope(Dispatchers.IO)

      beforeEach {
        deserializeHook = { }
        acknowledgeHook = { }
        negativeAcknowledgeHook = { }
        receivedList.clear()
        deserializedList.clear()
        processedList.clear()
        acknowledgedList.clear()
        negativeAcknowledgedList.clear()
      }

      "Processor stops when current scope is canceled, all ongoing messages should be processed" {
        val consumer = IntConsumer()
        val scope = getScope()

        later { scope.cancel() }

        scope.launch {
          coroutineScope {
            startProcessingWithoutKey(
                logger = logger,
                consumer = consumer,
                concurrency = Random.nextInt(5, 10),
                processor = ::process,
            )
          }
        }.join()

        println(processedList.sorted())
        receivedList.size shouldBeGreaterThan 0
        negativeAcknowledgedList.size shouldBeLessThanOrEqual 1
        acknowledgedList.sorted() shouldBe deserializedList.sorted()
        (acknowledgedList + negativeAcknowledgedList).sorted() shouldBe receivedList.sorted()
      }


      "An Error during deserialization triggers quitting after finishing processing all messages" {
        val consumer = IntConsumer()
        deserializeHook = { if (it.value == 100) throw Error("Expected Error") }

        getScope().launch {
          coroutineScope {
            startProcessingWithoutKey(
                logger = logger,
                consumer = consumer,
                concurrency = Random.nextInt(5, 10),
                processor = ::process,
            )
          }
        }.join()

        acknowledgedList shouldContainAll (1..90).toList()
        acknowledgedList shouldNotContain 100
        negativeAcknowledgedList shouldContain 100
        processedList.sorted() shouldBe deserializedList.sorted()
        (acknowledgedList + negativeAcknowledgedList).sorted() shouldBe receivedList.sorted()
      }

      "An Error during processing triggers quitting after finishing processing all messages" {
        val consumer = IntConsumer()
        suspend fun processWithError(
          message: Int,
          publishTime: MillisInstant
        ) = if (message == 100) throw Error("Expected Error") else process(message, publishTime)


        getScope().launch {
          coroutineScope {
            startProcessingWithoutKey(
                logger = logger,
                consumer = consumer,
                concurrency = Random.nextInt(5, 10),
                processor = ::processWithError,
            )
          }
        }.join()

        acknowledgedList shouldContainAll (1..90).toList()
        acknowledgedList shouldNotContain 100
        negativeAcknowledgedList shouldContain 100
        acknowledgedList.sorted() shouldBe processedList.sorted()
        (acknowledgedList + negativeAcknowledgedList).sorted() shouldBe receivedList.sorted()
      }

      "An Error during acknowledging triggers quitting after finishing processing all messages" {
        val consumer = IntConsumer()
        acknowledgeHook = { if (it.value == 100) throw Error("Expected Error") }

        getScope().launch {
          coroutineScope {
            startProcessingWithoutKey(
                logger = logger,
                consumer = consumer,
                concurrency = Random.nextInt(5, 10),
                processor = ::process,
            )
          }
        }.join()

        acknowledgedList shouldContainAll (1..90).toList()
        acknowledgedList shouldNotContain 100
        negativeAcknowledgedList shouldNotContain 100
        (acknowledgedList + 100).sorted() shouldBe processedList.sorted()
        (acknowledgedList + negativeAcknowledgedList + 100).sorted() shouldBe receivedList.sorted()
      }

      "An Error during negative acknowledging triggers quitting after finishing processing all messages" {
        val consumer = IntConsumer()
        deserializeHook = { if (it.value == 100) throw Exception("Expected Error") }
        negativeAcknowledgeHook = { if (it.value == 100) throw Error("Expected Error") }

        getScope().launch {
          coroutineScope {
            startProcessingWithoutKey(
                logger = logger,
                consumer = consumer,
                concurrency = Random.nextInt(5, 10),
                processor = ::process,
            )
          }
        }.join()

        acknowledgedList shouldContainAll (1..90).toList()
        acknowledgedList shouldNotContain 100
        negativeAcknowledgedList shouldNotContain 100
        (acknowledgedList + negativeAcknowledgedList + 100).sorted() shouldBe receivedList.sorted()
      }

      "An exception during deserialization triggers negative acknowledgment, but not exiting" {
        val consumer = IntConsumer()
        deserializeHook = {
          when (it.value) {
            100 -> throw Exception("Expected Exception")
            200 -> throw Error("Expected Error")
            else -> Unit
          }
        }

        getScope().launch {
          coroutineScope {
            startProcessingWithoutKey(
                logger = logger,
                consumer = consumer,
                concurrency = Random.nextInt(5, 10),
                processor = ::process,
            )
          }
        }.join()

        acknowledgedList.size shouldBeGreaterThanOrEqual 180
        deserializedList.sorted() shouldBe acknowledgedList.sorted()
        acknowledgedList shouldNotContain 100
        negativeAcknowledgedList shouldContain 100
        (acknowledgedList + negativeAcknowledgedList).sorted() shouldBe receivedList.sorted()
      }

      "An exception during processing triggers negative acknowledgment, but not exiting" {
        val consumer = IntConsumer()
        suspend fun processWithException(message: Int, publishedAt: MillisInstant) =
            when (message) {
              100 -> throw Exception("Expected Exception")
              200 -> throw Error("Expected Error")
              else -> process(message, publishedAt)
            }

        getScope().launch {
          coroutineScope {
            startProcessingWithoutKey(
                logger = logger,
                consumer = consumer,
                concurrency = Random.nextInt(5, 10),
                processor = ::processWithException,
            )
          }
        }.join()

        acknowledgedList.size shouldBeGreaterThanOrEqual 180
        processedList.sorted() shouldBe acknowledgedList.sorted()
        acknowledgedList shouldNotContain 100
        negativeAcknowledgedList shouldContain 100
        (acknowledgedList + negativeAcknowledgedList).sorted() shouldBe receivedList.sorted()
      }

      "An Exception during acknowledging does not trigger exiting" {
        val consumer = IntConsumer()
        acknowledgeHook = {
          if (it.value == 100) throw Exception("Expected Error")
          if (it.value == 200) throw Error("Expected Error")
        }

        getScope().launch {
          coroutineScope {
            startProcessingWithoutKey(
                logger = logger,
                consumer = consumer,
                concurrency = Random.nextInt(5, 10),
                processor = ::process,
            )
          }
        }.join()

        acknowledgedList.size shouldBeGreaterThan 180
        acknowledgedList shouldNotContain 100
        negativeAcknowledgedList shouldNotContain 100
        (acknowledgedList + 100 + 200).sorted() shouldBe processedList.sorted()
        (acknowledgedList + negativeAcknowledgedList + 100 + 200).sorted() shouldBe receivedList.sorted()
      }

      "An Exception during negative acknowledging does not trigger exiting" {
        val consumer = IntConsumer()
        deserializeHook = { if (it.value == 100) throw Exception("Expected Error") }
        negativeAcknowledgeHook = { if (it.value == 100) throw Exception("Expected Error") }
        acknowledgeHook = { if (it.value == 200) throw Error("Expected Error") }

        getScope().launch {
          coroutineScope {
            startProcessingWithoutKey(
                logger = logger,
                consumer = consumer,
                concurrency = Random.nextInt(5, 10),
                processor = ::process,
            )
          }
        }.join()

        acknowledgedList.size shouldBeGreaterThan 180
        acknowledgedList shouldNotContain 100
        negativeAcknowledgedList shouldNotContain 100
        (acknowledgedList + negativeAcknowledgedList + 100 + 200).sorted() shouldBe receivedList.sorted()
      }
    },
)
