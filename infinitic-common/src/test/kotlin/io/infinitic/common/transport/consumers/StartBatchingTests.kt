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
import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.random.Random

internal class StartBatchingTests : StringSpec(
    {
      val logger = KotlinLogging.logger {}
      fun getScope() = CoroutineScope(Dispatchers.IO)

      "should be able to batch by max message, up to scope cancellation" {
        with(logger) {
          val scope = getScope()
          val channel = with(scope) { IntConsumer().startConsuming() }
          val outputChannel = Channel<OneOrMany<Result<IntMessage, IntMessage>>>()

          channel.startBatching(5, 100, outputChannel)

          // batching
          shouldNotThrowAny {
            repeat(Random.nextInt(10, 50)) {
              val result = outputChannel.receive()
              result.shouldBeInstanceOf<Many<Result<IntMessage, IntMessage>>>()
              result.data.size shouldBe 5
            }
          }
          scope.isActive shouldBe true

          // after scope cancelling
          scope.cancel()
          // consumer channel should be closed
          shouldThrow<ClosedReceiveChannelException> { while (true) channel.receive() }
          // outputChannel should be closed
          shouldThrow<ClosedReceiveChannelException> {
            while (true) {
              outputChannel.receive()
            }
          }
        }
      }

      "should be able to batch by max duration, up to scope cancellation" {
        class SlowConsumer : IntConsumer() {
          override fun receiveAsync() = super.receiveAsync().thenApply {
            it.also { Thread.sleep(70) }
          }
        }

        with(logger) {
          val scope = getScope()
          val channel = with(scope) { SlowConsumer().startConsuming() }
          val outputChannel = Channel<OneOrMany<Result<IntMessage, IntMessage>>>()

          channel.startBatching(5, 100, outputChannel)

          // batching
          shouldNotThrowAny {
            repeat(Random.nextInt(2, 5)) {
              val result = outputChannel.receive()
              result.shouldBeInstanceOf<Many<Result<IntMessage, IntMessage>>>()
              result.data.size shouldBe 2
            }
          }
          scope.isActive shouldBe true

          // after scope cancelling
          scope.cancel()
          // consumer channel should be closed
          shouldThrow<ClosedReceiveChannelException> { while (true) channel.receive() }
          // outputChannel should be closed
          shouldThrow<ClosedReceiveChannelException> { while (true) outputChannel.receive() }
        }
      }

      "should be ablen" {
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
    },
)
