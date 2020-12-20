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

package io.infinitic.client

import io.infinitic.client.samples.FakeClass
import io.infinitic.client.samples.FakeInterface
import io.infinitic.client.samples.FakeTask
import io.infinitic.common.data.methods.MethodInput
import io.infinitic.common.data.methods.MethodName
import io.infinitic.common.data.methods.MethodParameterTypes
import io.infinitic.common.tasks.data.TaskId
import io.infinitic.common.tasks.data.TaskMeta
import io.infinitic.common.tasks.data.TaskName
import io.infinitic.common.tasks.data.TaskOptions
import io.infinitic.common.tasks.engine.messages.DispatchTask
import io.infinitic.common.tasks.engine.messages.TaskEngineMessage
import io.infinitic.common.workflows.engine.messages.WorkflowEngineMessage
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.mockk.slot

class ClientTaskTests : StringSpec({
    val taskSlot = slot<TaskEngineMessage>()
    val workflowSlot = slot<WorkflowEngineMessage>()
    val client = InfiniticClient(MockClientOutput(taskSlot, workflowSlot))

    beforeTest {
        taskSlot.clear()
        workflowSlot.clear()
    }

    "Should be able to dispatch method without parameter" {
        // when
        val taskId = TaskId(client.startTask<FakeTask> { m1() })
        // then
        taskSlot.isCaptured shouldBe true
        val msg = taskSlot.captured
        msg shouldBe DispatchTask(
            taskId = taskId,
            taskName = TaskName(FakeTask::class.java.name),
            methodName = MethodName("m1"),
            methodParameterTypes = MethodParameterTypes(listOf()),
            methodInput = MethodInput(),
            workflowId = null,
            methodRunId = null,
            taskOptions = TaskOptions(),
            taskMeta = TaskMeta()
        )
    }

    "Should be able to dispatch a method with a primitive as parameter" {
        // when
        val taskId = TaskId(client.startTask<FakeTask> { m1(0) })
        // then
        taskSlot.isCaptured shouldBe true
        val msg = taskSlot.captured
        msg shouldBe DispatchTask(
            taskId = taskId,
            taskName = TaskName(FakeTask::class.java.name),
            methodName = MethodName("m1"),
            methodParameterTypes = MethodParameterTypes(listOf(Int::class.java.name)),
            methodInput = MethodInput.from(0),
            workflowId = null,
            methodRunId = null,
            taskOptions = TaskOptions(),
            taskMeta = TaskMeta()
        )
    }

    "Should be able to dispatch a method with null as parameter" {
        // when
        val taskId = TaskId(client.startTask<FakeTask> { m1(null) })
        // then
        taskSlot.isCaptured shouldBe true
        val msg = taskSlot.captured
        msg shouldBe DispatchTask(
            taskId = taskId,
            taskName = TaskName(FakeTask::class.java.name),
            methodName = MethodName("m1"),
            methodParameterTypes = MethodParameterTypes(listOf(String::class.java.name)),
            methodInput = MethodInput.from(null),
            workflowId = null,
            methodRunId = null,
            taskOptions = TaskOptions(),
            taskMeta = TaskMeta()
        )
    }

    "Should be able to dispatch a method with multiple definition" {
        // when
        val taskId = TaskId(client.startTask<FakeTask> { m1("a") })
        // then
        taskSlot.isCaptured shouldBe true
        val msg = taskSlot.captured
        msg shouldBe DispatchTask(
            taskId = taskId,
            taskName = TaskName(FakeTask::class.java.name),
            methodName = MethodName("m1"),
            methodParameterTypes = MethodParameterTypes(listOf(String::class.java.name)),
            methodInput = MethodInput.from("a"),
            workflowId = null,
            methodRunId = null,
            taskOptions = TaskOptions(),
            taskMeta = TaskMeta()
        )
    }

    "Should be able to dispatch a method with multiple parameters" {
        // when
        val taskId = TaskId(client.startTask<FakeTask> { m1(0, "a") })
        // then
        taskSlot.isCaptured shouldBe true
        val msg = taskSlot.captured
        msg shouldBe DispatchTask(
            taskId = taskId,
            taskName = TaskName(FakeTask::class.java.name),
            methodName = MethodName("m1"),
            methodParameterTypes = MethodParameterTypes(listOf(Int::class.java.name, String::class.java.name)),
            methodInput = MethodInput.from(0, "a"),
            workflowId = null,
            methodRunId = null,
            taskOptions = TaskOptions(),
            taskMeta = TaskMeta()
        )
    }

    "Should be able to dispatch a method with an interface as parameter" {
        // when
        val fake = FakeClass()
        val taskId = TaskId(client.startTask<FakeTask> { m1(fake) })
        // then
        taskSlot.isCaptured shouldBe true
        val msg = taskSlot.captured

        msg shouldBe DispatchTask(
            taskId = taskId,
            taskName = TaskName(FakeTask::class.java.name),
            methodName = MethodName("m1"),
            methodParameterTypes = MethodParameterTypes(listOf(FakeInterface::class.java.name)),
            methodInput = MethodInput.from(fake),
            workflowId = null,
            methodRunId = null,
            taskOptions = TaskOptions(),
            taskMeta = TaskMeta()
        )
    }

    "Should be able to dispatch a method with a primitive as return value" {
        // when
        val taskId = TaskId(client.startTask<FakeTask> { m2() })
        // then
        taskSlot.isCaptured shouldBe true
        val msg = taskSlot.captured

        msg shouldBe DispatchTask(
            taskId = taskId,
            taskName = TaskName(FakeTask::class.java.name),
            methodName = MethodName("m2"),
            methodParameterTypes = MethodParameterTypes(listOf()),
            methodInput = MethodInput(),
            workflowId = null,
            methodRunId = null,
            taskOptions = TaskOptions(),
            taskMeta = TaskMeta()
        )
    }

    // TODO: add tests for cancel method

    // TODO: add tests for retry method

    // TODO: add tests for options

    // TODO: add tests for meta

    // TODO: add tests for error cases
})
