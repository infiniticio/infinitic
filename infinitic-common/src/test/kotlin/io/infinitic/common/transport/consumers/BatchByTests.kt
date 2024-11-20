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
import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.doubles.shouldBeLessThan
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlin.math.abs
import kotlin.random.Random

internal class BatchByTests : StringSpec(
    {
      val logger = KotlinLogging.logger("io.infinitic.tests")
      fun getScope() = CoroutineScope(Dispatchers.IO)

      "should be able to batch by max message, up to scope cancellation" {
        with(logger) {
          val size = Random.nextInt(5, 15)
          val scope = getScope()
          val channel = scope.startReceiving(IntConsumer())
          val outputChannel = with(scope) { channel.batchBy(size, 10000) }

          // batching
          shouldNotThrowAny {
            repeat(10) {
              val result: Result<List<IntMessage>, List<IntMessage>> = outputChannel.receive()
              println("receiving $result")
              result.data.size shouldBe size
            }
          }
          scope.isActive shouldBe true

          // after scope cancelling
          scope.cancel()

          // consumer channel should be closed
          shouldThrow<ClosedReceiveChannelException> { while (true) channel.receive() }
          // output channel should be closed
          shouldThrow<ClosedReceiveChannelException> { while (true) outputChannel.receive() }
        }
      }

      "should be able to batch by max duration, up to scope cancellation" {
        val size = 3
        val maxMillis = 500L
        val delay = 100L

        class SlowConsumer : IntConsumer() {
          override suspend fun batchReceive() = List(size) { receive() }.also { delay(delay) }
        }

        with(logger) {
          val scope = getScope()
          val channel = scope
              .startBatchReceiving(SlowConsumer())
              .process { _, m -> m.map { it.deserialize() } }
          val outputChannel = with(scope) {
            channel.batchBy(100, maxMillis) { i -> (i % 2).toString() }
          }

          // batching
          shouldNotThrowAny {
            repeat(2) {
              val result = outputChannel.receive()
              println("receiving $result")
              // check batch size
              abs(result.data.size - (maxMillis * size) / (2.0 * delay)) shouldBeLessThan 1.0
              // check batch by key
              val firstIsEven = result.data[0] % 2
              result.data.all { it % 2 == firstIsEven } shouldBe true
            }
          }
          scope.isActive shouldBe true

          // after scope cancelling
          scope.cancel()
          // consumer channel should be closed
          shouldThrow<ClosedReceiveChannelException> {
            while (true) {
              println("channel: " + channel.receive()); delay(delay)
            }
          }
          // output channel should be closed
          shouldThrow<ClosedReceiveChannelException> {
            while (true) {
              println("outputChannel: " + outputChannel.receive()); delay(delay)
            }
          }
        }
      }
    },
)
