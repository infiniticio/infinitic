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
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlinx.coroutines.isActive

internal class StartConsumingTests : StringSpec(
    {
      val logger = KotlinLogging.logger {}

      fun getScope() = CoroutineScope(Dispatchers.IO)

      "Consumer should consume up to scope cancellation" {
        with(logger) {
          val scope = getScope()
          val channel = with(scope) { IntConsumer().startConsuming(false) }

          // while no error
          shouldNotThrowAny { repeat(100) { channel.receive() } }
          scope.isActive shouldBe true

          // after scope cancelling
          scope.cancel()
          // Note: before triggering the exception, the channel will serve previously sent messages
          shouldThrow<ClosedReceiveChannelException> { while (true) channel.receive() }
        }
      }

      "Error in receiveAsync should close the channel and cancel the scope" {
        class ErrorConsumer : IntConsumer() {
          override suspend fun receive() = super.receive().also {
            if (it.value == 100) throw Error("Expected Error")
          }
        }

        with(logger) {
          val scope = getScope()
          val channel = with(scope) { ErrorConsumer().startConsuming(false) }

          // while no error
          shouldNotThrowAny { repeat(98) { channel.receive() } }
          scope.isActive shouldBe true

          // channel should be close to receive and scope should be canceled
          shouldThrow<ClosedReceiveChannelException> {
            // note: the exception is thrown at the 99th receive,
            // when the consumer loads an additional item
            channel.receive()
            channel.receive()
          }
          scope.isActive shouldBe false
        }
      }

      "Exception in receiveAsync should not close the channel neither cancel the scope" {
        class ExceptionConsumer : IntConsumer() {
          override suspend fun receive() = super.receive().also {
            if (it.value == 100) throw Exception("Expected Exception")
          }
        }

        with(logger) {
          val scope = getScope()
          val channel = with(scope) { ExceptionConsumer().startConsuming(false) }

          shouldNotThrowAny { repeat(100) { channel.receive() } }
          scope.isActive shouldBe true
          // clean up
          scope.cancel()
        }
      }
    },
)
