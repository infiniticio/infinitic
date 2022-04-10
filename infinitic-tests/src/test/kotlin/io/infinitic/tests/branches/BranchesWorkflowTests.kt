/**
 * "Commons Clause" License Condition v1.0
 *
 * The Software is provided to you by the Licensor under the License, as defined
 * below, subject to the following condition.
 *
 * Without limiting other conditions in the License, the grant of rights under the
 * License will not include, and the License does not grant to you, the right to
 * Sell the Software.
 *
 * For purposes of the foregoing, “Sell” means practicing any or all of the rights
 * granted to you under the License to provide to third parties, for a fee or
 * other consideration (including without limitation fees for hosting or
 * consulting/ support services related to the Software), a product or service
 * whose value derives, entirely or substantially, from the functionality of the
 * Software. Any license notice or attribution required by the License must also
 * include this Commons Clause License Condition notice.
 *
 * Software: Infinitic
 *
 * License: MIT License (https://opensource.org/licenses/MIT)
 *
 * Licensor: infinitic.io
 */

package io.infinitic.tests.branches

import io.infinitic.common.fixtures.later
import io.infinitic.factory.InfiniticClientFactory
import io.infinitic.factory.InfiniticWorkerFactory
import io.infinitic.tests.utils.UtilWorkflow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

internal class BranchesWorkflowTests : StringSpec({

    // each test should not be longer than 10s
    timeout = 10000

    val worker = autoClose(InfiniticWorkerFactory.fromConfigResource("/pulsar.yml"))
    val client = autoClose(InfiniticClientFactory.fromConfigResource("/pulsar.yml"))

    val branchesWorkflow = client.newWorkflow(BranchesWorkflow::class.java)
    val utilWorkflow = client.newWorkflow(UtilWorkflow::class.java)

    beforeSpec {
        worker.startAsync()
    }

    beforeTest {
        worker.storageFlush()
    }

    "Sequential Workflow with an async branch" {
        branchesWorkflow.seq3() shouldBe "23ba"
    }

    "Sequential Workflow with an async branch with 2 tasks" {
        branchesWorkflow.seq4() shouldBe "23bac"
    }

    "Test Deferred methods" {
        branchesWorkflow.deferred1() shouldBe "truefalsefalsetrue"
    }

    "Check runBranch" {
        val deferred = client.dispatch(utilWorkflow::receive, "a")

        val uw = client.getWorkflowById(UtilWorkflow::class.java, deferred.id)

        uw.concat("b") shouldBe "ab"

        later { uw.channelA.send("c") }

        deferred.await() shouldBe "abc"
    }

    "Check multiple runBranch" {
        val deferred1 = client.dispatch(utilWorkflow::receive, "a")
        val w = client.getWorkflowById(UtilWorkflow::class.java, deferred1.id)

        client.dispatch(w::add, "b")
        client.dispatch(w::add, "c")
        client.dispatch(w::add, "d")

        later { w.channelA.send("e") }

        deferred1.await() shouldBe "abcde"
    }

    "Check numerous runBranch" {
        val deferred1 = client.dispatch(utilWorkflow::receive, "a")
        val w = client.getWorkflowById(UtilWorkflow::class.java, deferred1.id)

        repeat(100) {
            client.dispatch(w::add, "b")
        }

        later { w.channelA.send("c") }

        deferred1.await() shouldBe "a" + "b".repeat(100) + "c"
    }
})
