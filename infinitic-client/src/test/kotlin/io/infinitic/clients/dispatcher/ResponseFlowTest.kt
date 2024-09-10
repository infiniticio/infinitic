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
package io.infinitic.clients.dispatcher

import io.infinitic.common.fixtures.later
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

internal class ResponseFlowTest : StringSpec(
    {
      "First get null if timeout" {
        val response = ResponseFlow<String>()

        response.first(100) shouldBe null
      }

      "First does not return element emitted before first call" {
        val response = ResponseFlow<String>()

        response.emit("foo")
        response.first(100) shouldBe null
      }

      "First returns element emitted after first() call" {
        val response = ResponseFlow<String>()

        later(10) {
          response.emit("foo")
        }
        response.first() shouldBe "foo"
      }

      "First returns element filtered" {
        val response = ResponseFlow<String>()

        later(10) {
          response.emit("foo1")
          response.emit("foo2")
          response.emit("foo3")
          response.emit("foo4")
          response.emit("foo5")
        }
        response.first { it == "foo3" } shouldBe "foo3"
      }

      "Can call multiple first() in parallel" {
        val response = ResponseFlow<String>()

        later(10) {
          response.emit("foo1")
          response.emit("foo2")
          response.emit("foo3")
          response.emit("foo4")
          response.emit("foo5")
        }
        coroutineScope {
          launch {
            response.first { it == "foo2" } shouldBe "foo2"
          }
          launch {
            response.first { it == "foo5" } shouldBe "foo5"
          }
        }
      }

      "can not retrieve the same element" {
        val response = ResponseFlow<String>()

        later(10) {
          response.emit("foo1")
          response.emit("foo2")
          response.emit("foo3")
          response.emit("foo4")
          response.emit("foo5")
        }
        response.first { it == "foo2" } shouldBe "foo2"
        // foo2 has been emitted before the next call
        response.first(100) { it == "foo2" } shouldBe null
      }

      "can retrieve the same element in parallel" {
        val response = ResponseFlow<String>()

        later(10) {
          response.emit("foo1")
          response.emit("foo2")
          response.emit("foo3")
          response.emit("foo4")
          response.emit("foo5")
        }
        coroutineScope {
          launch {
            response.first { it == "foo2" } shouldBe "foo2"
          }
          launch {
            response.first { it == "foo2" } shouldBe "foo2"
          }
        }
      }

      "Once an exception is thrown, we can not retrieve anything anymore" {
        val response = ResponseFlow<String>()
        val e = RuntimeException()

        response.emitThrowable(e)

        try {
          response.first()
        } catch (t: Throwable) {
          t shouldBe e
        }
        shouldThrow<RuntimeException> { response.first() } shouldBe e
      }
    },
)
