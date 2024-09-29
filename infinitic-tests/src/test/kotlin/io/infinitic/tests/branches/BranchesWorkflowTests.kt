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
package io.infinitic.tests.branches

import io.infinitic.Test
import io.infinitic.common.fixtures.later
import io.infinitic.getWorkflowState
import io.infinitic.utils.UtilWorkflow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.seconds

internal class BranchesWorkflowTests : StringSpec(
    {
      val client = Test.client
      val worker = Test.worker

      val branchesWorkflow = client.newWorkflow(BranchesWorkflow::class.java)
      val utilWorkflow = client.newWorkflow(UtilWorkflow::class.java)

      "Sequential Workflow with an async branch" {
        branchesWorkflow.seq3() shouldBe "23ba"

        worker.getWorkflowState() shouldBe null
      }

      "Sequential Workflow with an async branch with 2 tasks" {
        branchesWorkflow.seq4() shouldBe "23bac"

        worker.getWorkflowState() shouldBe null
      }

      "Test Deferred methods" {
        branchesWorkflow.deferred1() shouldBe "truefalsefalsetrue"

        worker.getWorkflowState() shouldBe null
      }
      
      "Check runBranch" {
        val deferred = client.dispatch(utilWorkflow::receive, "a")

        val uw = client.getWorkflowById(UtilWorkflow::class.java, deferred.id)

        uw.concat("b") shouldBe "ab"

        later { uw.channelA.send("c") }

        deferred.await() shouldBe "abc"

        worker.getWorkflowState(UtilWorkflow::class.java.name, deferred.id) shouldBe null
      }

      "Check multiple runBranch" {
        val deferred = client.dispatch(utilWorkflow::receive, "a")
        val w = client.getWorkflowById(UtilWorkflow::class.java, deferred.id)

        client.dispatch(w::add, "b")
        client.dispatch(w::add, "c")
        client.dispatch(w::add, "d")

        later { w.channelA.send("e") }

        deferred.await() shouldBe "abcde"

        worker.getWorkflowState(UtilWorkflow::class.java.name, deferred.id) shouldBe null
      }

      "Check numerous runBranch".config(timeout = 10.seconds) {
        val deferred = client.dispatch(utilWorkflow::receive, "a")
        val w = client.getWorkflowById(UtilWorkflow::class.java, deferred.id)

        repeat(100) { client.dispatch(w::add, "b") }

        later { w.channelA.send("c") }

        deferred.await() shouldBe "a" + "b".repeat(100) + "c"

        worker.getWorkflowState(UtilWorkflow::class.java.name, deferred.id) shouldBe null
      }

      "Check that state is cleaned after async processing of a branch" {
        branchesWorkflow.async1()

        // wait completion of the async branch
        delay(1500)

        worker.getWorkflowState() shouldBe null
      }
    },
)
