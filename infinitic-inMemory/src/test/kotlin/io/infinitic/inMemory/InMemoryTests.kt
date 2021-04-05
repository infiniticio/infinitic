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

package io.infinitic.inMemory

import io.infinitic.client.Client
import io.infinitic.common.clients.data.ClientName
import io.infinitic.inMemory.tasks.TaskA
import io.infinitic.inMemory.tasks.TaskAImpl
import io.infinitic.inMemory.workflows.WorkflowA
import io.infinitic.inMemory.workflows.WorkflowAImpl
import io.infinitic.inMemory.workflows.WorkflowB
import io.infinitic.inMemory.workflows.WorkflowBImpl
import io.infinitic.tasks.executor.register.TaskExecutorRegisterImpl
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

val client = Client(ClientName("client: inMemory"))

internal class InMemoryTests : StringSpec({

    beforeSpec {
        val taskExecutorRegister = TaskExecutorRegisterImpl().also {
            it.register(TaskA::class.java.name) { TaskAImpl() }
            it.register(WorkflowA::class.java.name) { WorkflowAImpl() }
            it.register(WorkflowB::class.java.name) { WorkflowBImpl() }
        }

        client.startInMemory(taskExecutorRegister)
    }

    afterSpec {
        client.close()
    }

    "simple synchronous task" {
        val taskA = client.newTask<TaskA>()

        val result = taskA.reverse("ab")

        result shouldBe "ba"
    }
})
