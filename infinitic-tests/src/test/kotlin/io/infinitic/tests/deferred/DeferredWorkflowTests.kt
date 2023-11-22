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
package io.infinitic.tests.deferred

import io.infinitic.tests.Test
import io.infinitic.tests.utils.UtilWorkflow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldBeIn
import io.kotest.matchers.shouldBe

internal class DeferredWorkflowTests :
  StringSpec(
      {
        val client = Test.client

        val deferredWorkflow =
            client.newWorkflow(DeferredWorkflow::class.java, tags = setOf("foo", "bar"))
        val utilWorkflow = client.newWorkflow(UtilWorkflow::class.java)

        "Simple Sequential Workflow" { deferredWorkflow.seq1() shouldBe "123" }

        "Wait for a dispatched Workflow" {
          val deferred = client.dispatch(deferredWorkflow::await, 200L)

          deferred.await() shouldBe 200L
        }

        "Simple sequential Workflow" { deferredWorkflow.seq1() shouldBe "123" }

        "Sequential Workflow with an async task" { deferredWorkflow.seq2() shouldBe "23ba" }

        "Workflow waiting 2 deferred in wrong order" { deferredWorkflow.seq5() shouldBe 600 }

        "Workflow waiting 2 deferred in wrong order followed by a step" {
          deferredWorkflow.seq6() shouldBe 600
        }

        "Or step with 3 async tasks" { deferredWorkflow.or1() shouldBeIn listOf("ba", "dc", "fe") }

        "Combined And/Or step with 3 async tasks" {
          deferredWorkflow.or2() shouldBeIn listOf(listOf("ba", "dc"), "fe")
        }

        "Or step with 3 async tasks through list" {
          deferredWorkflow.or3() shouldBeIn listOf("ba", "dc", "fe")
        }

        "Or step with Status checking" { deferredWorkflow.or4() shouldBe "baba" }

        "And step with 3 async tasks" { deferredWorkflow.and1() shouldBe listOf("ba", "dc", "fe") }

        "And step with 3 async tasks through list" {
          deferredWorkflow.and2() shouldBe listOf("ba", "dc", "fe")
        }

        "And step with 3 async tasks through large list" {
          deferredWorkflow.and3() shouldBe MutableList(20) { "ba" }
        }

        "Check multiple sync" {
          val result1 = deferredWorkflow.seq1()
          val result2 = utilWorkflow.concat("ok")

          result1 shouldBe "123"
          result2 shouldBe "ok"
        }
      },
  )
