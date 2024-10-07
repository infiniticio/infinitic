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
package io.infinitic.tests.batches

import io.infinitic.Test
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import kotlin.time.Duration.Companion.minutes

internal class BatchesWorkflowTests : StringSpec(
    {
      val client = Test.client
      val worker = Test.worker

      val batchWorkflow = client.newWorkflow(BatchWorkflow::class.java)

      // the first test has a large timeout to deal with Pulsar initialization
      "One primitive parameter (with maxSize=10)".config(timeout = 1.minutes) {
        for (i in 1..9) {
          client.dispatch(batchWorkflow::add, i)
        }

        batchWorkflow.add(10) shouldBe 1 + 2 + 3 + 4 + 5 + 6 + 7 + 8 + 9 + 10
      }

      "One primitive parameter (with maxDelaySeconds=1)" {
        for (i in 1..8) {
          client.dispatch(batchWorkflow::add, i)
        }

        batchWorkflow.add(9) shouldBe 1 + 2 + 3 + 4 + 5 + 6 + 7 + 8 + 9
      }

      "Two primitive parameters (with maxSize=10)" {
        for (i in 1..9) {
          client.dispatch(batchWorkflow::foo2, i, i)
        }

        batchWorkflow.foo2(10, 10) shouldBe (1 + 2 + 3 + 4 + 5 + 6 + 7 + 8 + 9 + 10) * 2
      }

      "Two primitive parameters (with maxDelaySeconds=1)" {
        for (i in 1..8) {
          client.dispatch(batchWorkflow::foo2, i, i)
        }

        batchWorkflow.foo2(9, 9) shouldBe (1 + 2 + 3 + 4 + 5 + 6 + 7 + 8 + 9) * 2
      }

      "One Object parameter (with maxSize=10)" {
        for (i in 1..9) {
          client.dispatch(batchWorkflow::foo3, i, i)
        }

        batchWorkflow.foo3(10, 10) shouldBe (1 + 2 + 3 + 4 + 5 + 6 + 7 + 8 + 9 + 10) * 2
      }

      "One Object parameter (with maxDelaySeconds=1)" {
        for (i in 1..8) {
          client.dispatch(batchWorkflow::foo3, i, i)
        }

        batchWorkflow.foo3(9, 9) shouldBe (1 + 2 + 3 + 4 + 5 + 6 + 7 + 8 + 9) * 2
      }

      "Returns Object (with maxSize=10)" {
        for (i in 1..9) {
          client.dispatch(batchWorkflow::foo4, i, i)
        }

        batchWorkflow.foo4(10, 10) shouldBe Input(foo = 55, bar = 10)
      }

      "Returns Object (with maxDelaySeconds=1)" {
        for (i in 1..8) {
          client.dispatch(batchWorkflow::foo4, i, i)
        }

        batchWorkflow.foo4(9, 9) shouldBe Input(foo = 45, bar = 9)
      }

      "One Object parameter and returns Object(with maxSize=10)" {
        for (i in 1..9) {
          client.dispatch(batchWorkflow::foo5, i, i)
        }

        batchWorkflow.foo5(10, 10) shouldBe Input(foo = 55 * 2, bar = 10)
      }

      "One Object parameter and returns Object (with maxDelaySeconds=1)" {
        for (i in 1..8) {
          client.dispatch(batchWorkflow::foo5, i, i)
        }

        batchWorkflow.foo5(9, 9) shouldBe Input(foo = 45 * 2, bar = 9)
      }
    },
)
