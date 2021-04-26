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
import io.infinitic.clients.Deferred
import io.infinitic.clients.getTask
import io.infinitic.clients.newTask
import io.infinitic.common.clients.data.ClientName
import io.infinitic.common.data.methods.MethodName
import io.infinitic.common.data.methods.MethodParameterTypes
import io.infinitic.common.data.methods.MethodParameters
import io.infinitic.common.fixtures.TestFactory
import io.infinitic.common.tasks.data.TaskId
import io.infinitic.common.tasks.data.TaskMeta
import io.infinitic.common.tasks.data.TaskName
import io.infinitic.common.tasks.data.TaskOptions
import io.infinitic.common.tasks.data.TaskTag
import io.infinitic.common.tasks.engine.messages.CancelTask
import io.infinitic.common.tasks.engine.messages.DispatchTask
import io.infinitic.common.tasks.engine.messages.RetryTask
import io.infinitic.common.tasks.engine.messages.TaskEngineMessage
import io.infinitic.common.tasks.tags.messages.AddTaskTag
import io.infinitic.common.tasks.tags.messages.CancelTaskPerTag
import io.infinitic.common.tasks.tags.messages.RetryTaskPerTag
import io.infinitic.common.tasks.tags.messages.TaskTagEngineMessage
import io.infinitic.common.workflows.engine.messages.WorkflowEngineMessage
import io.infinitic.common.workflows.tags.messages.WorkflowTagEngineMessage
import io.infinitic.exceptions.clients.CanNotApplyOnNewTaskStubException
import io.infinitic.exceptions.clients.MultipleMethodCallsException
import io.infinitic.exceptions.clients.NoMethodCallException
import io.infinitic.exceptions.clients.NotAStubException
import io.infinitic.exceptions.clients.SuspendMethodNotSupportedException
import io.kotest.assertions.throwables.shouldNotThrow
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.mockk.slot
import kotlinx.coroutines.coroutineScope
import java.util.UUID

private val taskTagSlots = mutableListOf<TaskTagEngineMessage>()
private val taskSlot = slot<TaskEngineMessage>()
private val workflowTagSlots = mutableListOf<WorkflowTagEngineMessage>()
private val workflowSlot = slot<WorkflowEngineMessage>()

class ClientTask : Client() {
    override val clientName = ClientName("clientTest")
    override val sendToTaskTagEngine = mockSendToTaskTagEngine(taskTagSlots)
    override val sendToTaskEngine = mockSendToTaskEngine(this, taskSlot)
    override val sendToWorkflowTagEngine = mockSendToWorkflowTagEngine(workflowTagSlots)
    override val sendToWorkflowEngine = mockSendToWorkflowEngine(this, workflowSlot)
    override fun close() {}
}

class ClientTaskTests : StringSpec({
    val client = ClientTask()

    beforeTest {
        taskTagSlots.clear()
        taskSlot.clear()
        workflowTagSlots.clear()
        workflowSlot.clear()
    }

    "Should throw when not using a stub" {
        shouldThrow<NotAStubException> {
            client.async("") { }
        }

        shouldThrow<NotAStubException> {
            client.retry("")
        }

        shouldThrow<NotAStubException> {
            client.cancel("")
        }
    }

    "Should not throw when re-using a stub" {
        // when
        val fakeTask = client.newTask<FakeTask>()
        shouldNotThrow<Throwable> {
            client.async(fakeTask) { m1() }
            client.async(fakeTask) { m1() }
        }
    }

    "Should throw when calling 2 methods" {
        // when
        val fakeTask = client.newTask<FakeTask>()
        shouldThrow<MultipleMethodCallsException> {
            client.async(fakeTask) { m1(); m1() }
        }
    }

    "Should throw when not calling any method" {
        // when
        val fakeTask = client.newTask<FakeTask>()
        shouldThrow<NoMethodCallException> {
            client.async(fakeTask) { }
        }
    }

    "Should throw when retrying new stub" {
        // when
        val fakeTask = client.newTask<FakeTask>()
        shouldThrow<CanNotApplyOnNewTaskStubException> {
            client.retry(fakeTask)
        }
    }

    "Should throw when canceling new stub" {
        // when
        val fakeTask = client.newTask<FakeTask>()
        shouldThrow<CanNotApplyOnNewTaskStubException> {
            client.cancel(fakeTask)
        }
    }

    "Should throw when using a suspend method" {
        // when
        val fakeTask = client.newTask<FakeTask>()
        shouldThrow<SuspendMethodNotSupportedException> {
            fakeTask.suspendedMethod()
        }
    }

    "Should be able to dispatch method without parameter" {
        // when
        val fakeTask = client.newTask<FakeTask>()
        val deferred: Deferred<Unit> = client.async(fakeTask) { m1() }
        // then
        taskTagSlots.size shouldBe 0
        taskSlot.captured shouldBe DispatchTask(
            clientName = client.clientName,
            clientWaiting = false,
            taskId = TaskId(deferred.id),
            taskName = TaskName(FakeTask::class.java.name),
            methodName = MethodName("m1"),
            methodParameterTypes = MethodParameterTypes(listOf()),
            methodParameters = MethodParameters(),
            workflowId = null,
            workflowName = null,
            methodRunId = null,
            taskTags = setOf(),
            taskOptions = TaskOptions(),
            taskMeta = TaskMeta()
        )
    }

    "Should be able to dispatch method without parameter (Java syntax)" {
        // when
        val fakeTask = client.newTask(FakeTask::class.java)
        val deferred: Deferred<Unit> = client.async(fakeTask) { m1() }
        // then
        taskTagSlots.size shouldBe 0
        taskSlot.captured shouldBe DispatchTask(
            clientName = client.clientName,
            clientWaiting = false,
            taskId = TaskId(deferred.id),
            taskName = TaskName(FakeTask::class.java.name),
            methodName = MethodName("m1"),
            methodParameterTypes = MethodParameterTypes(listOf()),
            methodParameters = MethodParameters(),
            workflowId = null,
            workflowName = null,
            methodRunId = null,
            taskTags = setOf(),
            taskOptions = TaskOptions(),
            taskMeta = TaskMeta()
        )
    }

    "Should be able to dispatch method with options and meta" {
        // when
        val options = TestFactory.random<TaskOptions>()
        val meta = mapOf(
            "foo" to TestFactory.random<ByteArray>(),
            "bar" to TestFactory.random<ByteArray>()
        )
        val fakeTask = client.newTask<FakeTask>(options = options, meta = meta)
        val deferred: Deferred<Unit> = client.async(fakeTask) { m1() }
        // then
        taskTagSlots.size shouldBe 0
        taskSlot.captured shouldBe DispatchTask(
            clientName = client.clientName,
            clientWaiting = false,
            taskId = TaskId(deferred.id),
            taskName = TaskName(FakeTask::class.java.name),
            methodName = MethodName("m1"),
            methodParameterTypes = MethodParameterTypes(listOf()),
            methodParameters = MethodParameters(),
            workflowId = null,
            workflowName = null,
            methodRunId = null,
            taskTags = setOf(),
            taskOptions = options,
            taskMeta = TaskMeta(meta)
        )
    }

    "Should be able to dispatch method with tags" {
        // when
        val fakeTask = client.newTask<FakeTask>(tags = setOf("foo", "bar"))
        val deferred: Deferred<Unit> = client.async(fakeTask) { m1() }
        // then
        taskTagSlots.size shouldBe 2
        taskTagSlots[0] shouldBe AddTaskTag(
            taskTag = TaskTag("foo"),
            taskName = TaskName(FakeTask::class.java.name),
            taskId = TaskId(deferred.id),
        )
        taskTagSlots[1] shouldBe AddTaskTag(
            taskTag = TaskTag("bar"),
            taskName = TaskName(FakeTask::class.java.name),
            taskId = TaskId(deferred.id),
        )
        taskSlot.captured shouldBe DispatchTask(
            clientName = client.clientName,
            clientWaiting = false,
            taskId = TaskId(deferred.id),
            taskName = TaskName(FakeTask::class.java.name),
            methodName = MethodName("m1"),
            methodParameterTypes = MethodParameterTypes(listOf()),
            methodParameters = MethodParameters(),
            workflowId = null,
            workflowName = null,
            methodRunId = null,
            taskTags = setOf(TaskTag("foo"), TaskTag("bar")),
            taskOptions = TaskOptions(),
            taskMeta = TaskMeta()
        )
    }

    "Should be able to dispatch a method with a primitive as parameter" {
        // when
        val fakeTask = client.newTask<FakeTask>()
        val deferred: Deferred<String> = client.async(fakeTask) { m1(0) }
        // then
        taskTagSlots.size shouldBe 0
        taskSlot.captured shouldBe DispatchTask(
            clientName = client.clientName,
            clientWaiting = false,
            taskId = TaskId(deferred.id),
            taskName = TaskName(FakeTask::class.java.name),
            methodName = MethodName("m1"),
            methodParameterTypes = MethodParameterTypes(listOf(Int::class.java.name)),
            methodParameters = MethodParameters.from(0),
            workflowId = null,
            workflowName = null,
            methodRunId = null,
            taskTags = setOf(),
            taskOptions = TaskOptions(),
            taskMeta = TaskMeta()
        )
    }

    "Should be able to dispatch a method with null as parameter" {
        // when
        val fakeTask = client.newTask<FakeTask>()
        val deferred = client.async(fakeTask) { m1(null) }
        // then
        taskTagSlots.size shouldBe 0
        taskSlot.captured shouldBe DispatchTask(
            clientName = client.clientName,
            clientWaiting = false,
            taskId = TaskId(deferred.id),
            taskName = TaskName(FakeTask::class.java.name),
            methodName = MethodName("m1"),
            methodParameterTypes = MethodParameterTypes(listOf(String::class.java.name)),
            methodParameters = MethodParameters.from(null),
            workflowId = null,
            workflowName = null,
            methodRunId = null,
            taskTags = setOf(),
            taskOptions = TaskOptions(),
            taskMeta = TaskMeta()
        )
    }

    "Should be able to dispatch a method with multiple definition" {
        // when
        val fakeTask = client.newTask<FakeTask>()
        val deferred = client.async(fakeTask) { m1("a") }
        // then
        taskTagSlots.size shouldBe 0
        taskSlot.captured shouldBe DispatchTask(
            clientName = client.clientName,
            clientWaiting = false,
            taskId = TaskId(deferred.id),
            taskName = TaskName(FakeTask::class.java.name),
            methodName = MethodName("m1"),
            methodParameterTypes = MethodParameterTypes(listOf(String::class.java.name)),
            methodParameters = MethodParameters.from("a"),
            workflowId = null,
            workflowName = null,
            methodRunId = null,
            taskTags = setOf(),
            taskOptions = TaskOptions(),
            taskMeta = TaskMeta()
        )
    }

    "Should be able to dispatch a method with multiple parameters" {
        // when
        val fakeTask = client.newTask<FakeTask>()
        val deferred = client.async(fakeTask) { m1(0, "a") }
        // then
        taskTagSlots.size shouldBe 0
        taskSlot.captured shouldBe DispatchTask(
            clientName = client.clientName,
            clientWaiting = false,
            taskId = TaskId(deferred.id),
            taskName = TaskName(FakeTask::class.java.name),
            methodName = MethodName("m1"),
            methodParameterTypes = MethodParameterTypes(listOf(Int::class.java.name, String::class.java.name)),
            methodParameters = MethodParameters.from(0, "a"),
            workflowId = null,
            workflowName = null,
            methodRunId = null,
            taskTags = setOf(),
            taskOptions = TaskOptions(),
            taskMeta = TaskMeta()
        )
    }

    "Should be able to dispatch a method with an interface as parameter" {
        // when
        val fakeTask = client.newTask<FakeTask>()
        val fake = FakeClass()
        val deferred = client.async(fakeTask) { m1(fake) }
        // then
        taskTagSlots.size shouldBe 0
        taskSlot.captured shouldBe DispatchTask(
            clientName = client.clientName,
            clientWaiting = false,
            taskId = TaskId(deferred.id),
            taskName = TaskName(FakeTask::class.java.name),
            methodName = MethodName("m1"),
            methodParameterTypes = MethodParameterTypes(listOf(FakeInterface::class.java.name)),
            methodParameters = MethodParameters.from(fake),
            workflowId = null,
            workflowName = null,
            methodRunId = null,
            taskTags = setOf(),
            taskOptions = TaskOptions(),
            taskMeta = TaskMeta()
        )
    }

    "Should be able to dispatch a method with a primitive as return value" {
        // when
        val fakeTask = client.newTask<FakeTask>()
        val deferred = client.async(fakeTask) { m2() }
        // then
        taskTagSlots.size shouldBe 0
        taskSlot.captured shouldBe DispatchTask(
            clientName = client.clientName,
            clientWaiting = false,
            taskId = TaskId(deferred.id),
            taskName = TaskName(FakeTask::class.java.name),
            methodName = MethodName("m2"),
            methodParameterTypes = MethodParameterTypes(listOf()),
            methodParameters = MethodParameters(),
            workflowId = null,
            workflowName = null,
            methodRunId = null,
            taskTags = setOf(),
            taskOptions = TaskOptions(),
            taskMeta = TaskMeta()
        )
    }

    "Should be able to dispatch a method synchronously" {
        // when
        val fakeTask = client.newTask<FakeTask>()
        var result: String
        coroutineScope {
            result = fakeTask.m1(0, "a")
        }
        // then
        result shouldBe "success"
        taskTagSlots.size shouldBe 0
        val msg = taskSlot.captured
        msg shouldBe DispatchTask(
            clientName = client.clientName,
            clientWaiting = true,
            taskId = msg.taskId,
            taskName = TaskName(FakeTask::class.java.name),
            methodName = MethodName("m1"),
            methodParameterTypes = MethodParameterTypes(listOf(Int::class.java.name, String::class.java.name)),
            methodParameters = MethodParameters.from(0, "a"),
            workflowId = null,
            workflowName = null,
            methodRunId = null,
            taskTags = setOf(),
            taskOptions = TaskOptions(),
            taskMeta = TaskMeta()
        )
    }

    "Should be able to await for a task just dispatched" {
        // when
        val fakeTask = client.newTask<FakeTask>()
        var result: String
        coroutineScope {
            val deferred = client.async(fakeTask) { m1(0, "a") }
            result = deferred.await()
        }
        // then
        result shouldBe "success"
    }

    "Should be able to cancel task per id" {
        // when
        val id = UUID.randomUUID()
        val fakeTask = client.getTask<FakeTask>(id)
        client.cancel(fakeTask)
        // then
        taskTagSlots.size shouldBe 0
        taskSlot.captured shouldBe CancelTask(
            taskId = TaskId(id),
            taskName = TaskName(FakeTask::class.java.name)
        )
    }

    "Should be able to cancel task per id with output" {
        // when
        val id = UUID.randomUUID()
        val fakeTask = client.getTask<FakeTask>(id)
        client.cancel(fakeTask)
        // then
        taskTagSlots.size shouldBe 0
        taskSlot.captured shouldBe CancelTask(
            taskId = TaskId(id),
            taskName = TaskName(FakeTask::class.java.name)
        )
    }

    "Should be able to cancel task per tag" {
        // when
        val fakeTask = client.getTask<FakeTask>("foo")
        client.cancel(fakeTask)
        // then
        taskTagSlots.size shouldBe 1
        taskTagSlots[0] shouldBe CancelTaskPerTag(
            taskTag = TaskTag("foo"),
            taskName = TaskName(FakeTask::class.java.name)
        )
        taskSlot.isCaptured shouldBe false
    }

    "Should be able to cancel task per tag with output" {
        // when
        val fakeTask = client.getTask<FakeTask>("foo")
        client.cancel(fakeTask)
        // then
        taskTagSlots.size shouldBe 1
        taskTagSlots[0] shouldBe CancelTaskPerTag(
            taskTag = TaskTag("foo"),
            taskName = TaskName(FakeTask::class.java.name)
        )
        taskSlot.isCaptured shouldBe false
    }

    "Should be able to cancel task just dispatched" {
        // when
        val fakeTask = client.newTask<FakeTask>()
        val deferred = client.async(fakeTask) { m1() }

        client.cancel(fakeTask)
        // then
        taskTagSlots.size shouldBe 0
        taskSlot.captured shouldBe CancelTask(
            taskId = TaskId(deferred.id),
            taskName = TaskName(FakeTask::class.java.name)
        )
    }

    "Should be able to retry task per id" {
        // when
        val id = UUID.randomUUID()
        val fakeTask = client.getTask<FakeTask>(id)
        client.retry(fakeTask)
        // then
        taskTagSlots.size shouldBe 0
        taskSlot.captured shouldBe RetryTask(
            taskId = TaskId(id),
            taskName = TaskName(FakeTask::class.java.name)
        )
    }

    "Should be able to retry task per tag" {
        // when
        val fakeTask = client.getTask<FakeTask>("foo")
        client.retry(fakeTask)
        // then
        taskTagSlots.size shouldBe 1
        taskTagSlots[0] shouldBe RetryTaskPerTag(
            taskTag = TaskTag("foo"),
            taskName = TaskName(FakeTask::class.java.name)
        )
        taskSlot.isCaptured shouldBe false
    }

    "Should be able to retry a task just dispatched " {
        // when
        val fakeTask = client.newTask<FakeTask>()
        val deferred = client.async(fakeTask) { m1() }

        client.retry(fakeTask)
        // then
        taskTagSlots.size shouldBe 0
        taskSlot.captured shouldBe RetryTask(
            taskId = TaskId(deferred.id),
            taskName = TaskName(FakeTask::class.java.name)
        )
    }
})
