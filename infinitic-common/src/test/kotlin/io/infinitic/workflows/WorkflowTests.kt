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

package io.infinitic.workflows

import io.infinitic.common.proxies.NewTaskProxyHandler
import io.infinitic.common.proxies.NewWorkflowProxyHandler
import io.infinitic.common.tasks.data.TaskMeta
import io.infinitic.common.tasks.data.TaskName
import io.infinitic.common.tasks.data.TaskTag
import io.infinitic.common.workflows.data.workflows.WorkflowMeta
import io.infinitic.common.workflows.data.workflows.WorkflowName
import io.infinitic.common.workflows.data.workflows.WorkflowTag
import io.infinitic.workflows.samples.TaskA
import io.infinitic.workflows.samples.WorkflowA
import io.infinitic.workflows.samples.WorkflowAImpl
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot

class WorkflowTests : StringSpec({
    val dispatcher = mockk<WorkflowDispatcher>()
    val newTaskSlot = slot<NewTaskProxyHandler<*>>()
    val newWorkflowSlot = slot<NewWorkflowProxyHandler<*>>()
    every { dispatcher.dispatchAndWait<Long>(capture(newTaskSlot)) } returns 100L
    every { dispatcher.dispatch<Long>(capture(newTaskSlot), false) } returns mockk()
    every { dispatcher.dispatchAndWait<Long>(capture(newWorkflowSlot)) } returns 100L
    every { dispatcher.dispatch<Long>(capture(newWorkflowSlot), false) } returns mockk()

    "Workflow should trigger synchronously a task" {
        val w = WorkflowAImpl().apply { this.dispatcher = dispatcher }

        w.syncTask(100)

        with(newTaskSlot.captured) {
            taskName shouldBe TaskName(TaskA::class.java.name)
            methodName.toString() shouldBe TaskA::await.name
            methodArgs.toList() shouldBe listOf(100)
            taskTags shouldBe setOf(TaskTag("foo"), TaskTag("bar"))
            taskMeta shouldBe TaskMeta(mapOf("foo" to "bar".toByteArray()))
        }
    }

    "Workflow should trigger asynchronously a task" {
        val w = WorkflowAImpl().apply { this.dispatcher = dispatcher }

        w.asyncTask(100)

        with(newTaskSlot.captured) {
            taskName shouldBe TaskName(TaskA::class.java.name)
            methodName.toString() shouldBe TaskA::await.name
            methodArgs.toList() shouldBe listOf(100)
            taskTags shouldBe setOf(TaskTag("foo"), TaskTag("bar"))
            taskMeta shouldBe TaskMeta(mapOf("foo" to "bar".toByteArray()))
        }
    }

    "Workflow should trigger synchronously a child-workflow" {
        val w = WorkflowAImpl().apply { this.dispatcher = dispatcher }

        w.syncWorkflow(100)

        with(newWorkflowSlot.captured) {
            workflowName shouldBe WorkflowName(WorkflowA::class.java.name)
            methodName.toString() shouldBe WorkflowA::syncTask.name
            methodArgs.toList() shouldBe listOf(100)
            workflowTags shouldBe setOf(WorkflowTag("foo"), WorkflowTag("bar"))
            workflowMeta shouldBe WorkflowMeta(mapOf("foo" to "bar".toByteArray()))
        }
    }

    "Workflow should trigger asynchronously a child-workflow" {
        val w = WorkflowAImpl().apply { this.dispatcher = dispatcher }

        w.asyncWorkflow(100)

        with(newWorkflowSlot.captured) {
            workflowName shouldBe WorkflowName(WorkflowA::class.java.name)
            methodName.toString() shouldBe WorkflowA::syncTask.name
            methodArgs.toList() shouldBe listOf(100)
            workflowTags shouldBe setOf(WorkflowTag("foo"), WorkflowTag("bar"))
            workflowMeta shouldBe WorkflowMeta(mapOf("foo" to "bar".toByteArray()))
        }
    }
})
