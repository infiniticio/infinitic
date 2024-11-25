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
import io.infinitic.common.fixtures.later
import io.infinitic.common.transport.logged.LoggerWithCounter
import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.random.Random

internal class StartBatchingTests : StringSpec(
    {
      val logger = LoggerWithCounter(KotlinLogging.logger("io.infinitic.tests"))
      fun getScope() = CoroutineScope(Dispatchers.IO)

      "test startBatching function" {
        with(KotlinLogging.logger {}) {
          val scope = CoroutineScope(Dispatchers.IO)

          val inputChannel = Channel<Result<String, Int>>()
          val outputChannel = Channel<Result<List<String>, List<Int>>>()

          // Start batching function in background
          scope.launch {
            inputChannel.startBatching(2, 1000L, outputChannel)
          }

          val job = scope.launch {
            // Expect batched output
            val output1 = outputChannel.receive()
            output1.message shouldBe listOf("Hello", "World")
            output1.data shouldBe listOf(1, 2)

            // Expect batched output
            val output2 = outputChannel.receive()
            output2.message shouldBe listOf("Foo", "Bar")
            output2.data shouldBe listOf(3, 4)
          }

          // Send data to input channel
          inputChannel.send(Result.success("Hello", 1))
          inputChannel.send(Result.success("World", 2))
          inputChannel.send(Result.success("Foo", 3))
          inputChannel.send(Result.success("Bar", 4))

          job.join()

          // Close input channel
          inputChannel.close()

          // Expect output channel to be closed as well
          shouldThrow<ClosedReceiveChannelException> { outputChannel.receive() }
        }
      }

      "should be able to batch by max message, up to scope cancellation" {
        with(logger) {
          val scope = getScope()
          val inputChannel = scope.startReceiving(IntConsumer())
          val outputChannel = inputChannel.startBatching(5, 100)

          // batching
          shouldNotThrowAny {
            repeat(Random.nextInt(10, 50)) {
              val result: Result<List<IntMessage>, List<IntMessage>> = outputChannel.receive()
              result.data.size shouldBe 5
            }
          }
          scope.isActive shouldBe true

          // after scope cancelling
          scope.cancel()
          // consumer channel should be closed
          shouldThrow<ClosedReceiveChannelException> { while (true) inputChannel.receive() }
          // outputChannel should be closed
          shouldThrow<ClosedReceiveChannelException> { while (true) outputChannel.receive() }
        }
      }

      "should be able to batch by max duration, up to scope cancellation" {
        class SlowConsumer : IntConsumer() {
          override suspend fun receive() = super.receive().also {
            delay(70)
          }
        }

        with(logger) {
          val scope = getScope()
          val inputChannel = scope.startReceiving(SlowConsumer())
          val outputChannel = inputChannel.startBatching(5, 100)

          // batching
          shouldNotThrowAny {
            repeat(Random.nextInt(2, 5)) {
              val result: Result<List<IntMessage>, List<IntMessage>> = outputChannel.receive()
              result.data.size shouldBe 2
            }
          }
          scope.isActive shouldBe true

          // after scope cancelling
          scope.cancel()
          // consumer channel should be closed
          shouldThrow<ClosedReceiveChannelException> { while (true) inputChannel.receive() }
          // outputChannel should be closed
          shouldThrow<ClosedReceiveChannelException> { while (true) outputChannel.receive() }
        }
      }

      "receiveIfNotClose should returns null when channel is closed" {
        with(logger) {
          val channel = Channel<Int>()
          val job = launch {
            while (true) {
              channel.receiveIfNotClose() ?: break
            }
          }

          later {
            channel.close()
          }

          job.join()
        }
      }
    },
)
