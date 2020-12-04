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
import io.infinitic.common.tasks.data.TaskMeta
import io.infinitic.common.tasks.data.TaskName
import io.infinitic.common.tasks.data.TaskOptions
import io.infinitic.common.tasks.engine.messages.DispatchTask
import io.infinitic.common.tasks.engine.messages.TaskEngineMessage
import io.infinitic.common.tasks.engine.transport.SendToTaskEngine
import io.infinitic.common.workflows.engine.SendToWorkflowEngine
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot

class ClientTaskTests : StringSpec({
    val sendToTaskEngine = mockk<SendToTaskEngine>()
    val sendToWorkflowEngine = mockk<SendToWorkflowEngine>()
    val slot = slot<TaskEngineMessage>()
    coEvery { sendToTaskEngine(capture(slot), 0F) } just Runs
    val client = Client(sendToTaskEngine, sendToWorkflowEngine)

    beforeTest {
        slot.clear()
    }

    "Should be able to dispatch method without parameter" {
        // when
        val task = client.dispatch(FakeTask::class.java) { m1() }
        // then
        slot.isCaptured shouldBe true
        val msg = slot.captured
        msg shouldBe DispatchTask(
            taskId = task.taskId,
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
        val task = client.dispatch(FakeTask::class.java) { m1(0) }
        // then
        slot.isCaptured shouldBe true
        val msg = slot.captured
        msg shouldBe DispatchTask(
            taskId = task.taskId,
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
        val task = client.dispatch(FakeTask::class.java) { m1(null) }
        // then
        slot.isCaptured shouldBe true
        val msg = slot.captured
        msg shouldBe DispatchTask(
            taskId = task.taskId,
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
        val task = client.dispatch(FakeTask::class.java) { m1("a") }
        // then
        slot.isCaptured shouldBe true
        val msg = slot.captured
        msg shouldBe DispatchTask(
            taskId = task.taskId,
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
        val task = client.dispatch(FakeTask::class.java) { m1(0, "a") }
        // then
        slot.isCaptured shouldBe true
        val msg = slot.captured
        msg shouldBe DispatchTask(
            taskId = task.taskId,
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
        val task = client.dispatch(FakeTask::class.java) { m1(fake) }
        // then
        slot.isCaptured shouldBe true
        val msg = slot.captured

        msg shouldBe DispatchTask(
            taskId = task.taskId,
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
        val task = client.dispatch(FakeTask::class.java) { m2() }
        // then
        slot.isCaptured shouldBe true
        val msg = slot.captured

        msg shouldBe DispatchTask(
            taskId = task.taskId,
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
