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

package io.infinitic.tests.inline

import io.infinitic.exceptions.FailedWorkflowException
import io.infinitic.exceptions.FailedWorkflowTaskException
import io.infinitic.exceptions.workflows.InvalidInlineException
import io.infinitic.factory.InfiniticClientFactory
import io.infinitic.factory.InfiniticWorkerFactory
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

internal class InlineWorkflowTests : StringSpec({

    // each test should not be longer than 10s
    timeout = 10000

    val worker = autoClose(InfiniticWorkerFactory.fromConfigResource("/pulsar.yml"))
    val client = autoClose(InfiniticClientFactory.fromConfigResource("/pulsar.yml"))

    val inlineWorkflow = client.newWorkflow(InlineWorkflow::class.java)

    beforeSpec {
        worker.startAsync()
    }

    beforeTest {
        worker.storageFlush()
    }

    "Inline task" {
        inlineWorkflow.inline1(7) shouldBe "2 * 7 = 14"
    }

    "Inline task with asynchronous task inside" {
        val error = shouldThrow<FailedWorkflowException> { inlineWorkflow.inline2(21) }

        val deferredException = error.deferredException as FailedWorkflowTaskException
        deferredException.workerException.name shouldBe InvalidInlineException::class.java.name
    }

    "Inline task with synchronous task inside" {
        val error = shouldThrow<FailedWorkflowException> { inlineWorkflow.inline3(14) }

        val deferredException = error.deferredException as FailedWorkflowTaskException
        deferredException.workerException.name shouldBe InvalidInlineException::class.java.name
    }
})
