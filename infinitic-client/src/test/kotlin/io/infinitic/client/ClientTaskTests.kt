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

import io.infinitic.client.deferred.Deferred
import io.infinitic.client.samples.FakeClass
import io.infinitic.client.samples.FakeInterface
import io.infinitic.client.samples.FakeTask
import io.infinitic.common.clients.data.ClientName
import io.infinitic.common.data.methods.MethodName
import io.infinitic.common.data.methods.MethodParameterTypes
import io.infinitic.common.data.methods.MethodParameters
import io.infinitic.common.data.methods.MethodReturnValue
import io.infinitic.common.fixtures.TestFactory
import io.infinitic.common.tags.data.Tag
import io.infinitic.common.tags.messages.AddTaskTag
import io.infinitic.common.tags.messages.CancelTaskPerTag
import io.infinitic.common.tags.messages.RetryTaskPerTag
import io.infinitic.common.tags.messages.TagEngineMessage
import io.infinitic.common.tasks.data.TaskId
import io.infinitic.common.tasks.data.TaskMeta
import io.infinitic.common.tasks.data.TaskName
import io.infinitic.common.tasks.data.TaskOptions
import io.infinitic.common.tasks.engine.messages.CancelTask
import io.infinitic.common.tasks.engine.messages.DispatchTask
import io.infinitic.common.tasks.engine.messages.RetryTask
import io.infinitic.common.tasks.engine.messages.TaskEngineMessage
import io.infinitic.common.workflows.engine.messages.WorkflowEngineMessage
import io.infinitic.exceptions.CanNotReuseTaskStub
import io.infinitic.exceptions.CanNotUseNewTaskStub
import io.infinitic.exceptions.MultipleMethodCalls
import io.infinitic.exceptions.NoMethodCall
import io.infinitic.exceptions.NotAStub
import io.infinitic.exceptions.SuspendMethodNotSupported
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.mockk.slot
import kotlinx.coroutines.coroutineScope
import java.util.UUID

class ClientTaskTests : StringSpec({
    val tagSlots = mutableListOf<TagEngineMessage>()
    val taskSlot = slot<TaskEngineMessage>()
    val workflowSlot = slot<WorkflowEngineMessage>()
    val client = Client(ClientName("clientTest"))

    client.setOutput(
        mockSendToTagEngine(tagSlots),
        mockSendToTaskEngine(client, taskSlot),
        mockSendToWorkflowEngine(client, workflowSlot)
    )

    beforeTest {
        tagSlots.clear()
        taskSlot.clear()
        workflowSlot.clear()
    }

    "Should throw when not using a stub" {
        shouldThrow<NotAStub> {
            client.async("") { }
        }

        shouldThrow<NotAStub> {
            client.retry("")
        }

        shouldThrow<NotAStub> {
            client.cancel("") { }
        }
    }

    "Should throw when re-using a stub" {
        // when
        val fakeTask = client.newTask<FakeTask>()
        shouldThrow<CanNotReuseTaskStub> {
            client.async(fakeTask) { m1() }
            client.async(fakeTask) { m1() }
        }
    }

    "Should throw when calling 2 methods" {
        // when
        val fakeTask = client.newTask<FakeTask>()
        shouldThrow<MultipleMethodCalls> {
            client.async(fakeTask) { m1(); m1() }
        }
    }

    "Should throw when not calling any method" {
        // when
        val fakeTask = client.newTask<FakeTask>()
        shouldThrow<NoMethodCall> {
            client.async(fakeTask) { }
        }
    }

    "Should throw when retrying new stub" {
        // when
        val fakeTask = client.newTask<FakeTask>()
        shouldThrow<CanNotUseNewTaskStub> {
            client.retry(fakeTask)
        }
    }

    "Should throw when canceling new stub" {
        // when
        val fakeTask = client.newTask<FakeTask>()
        shouldThrow<CanNotUseNewTaskStub> {
            client.cancel(fakeTask)
        }
    }

    "Should throw when using a suspend method" {
        // when
        val fakeTask = client.newTask<FakeTask>()
        shouldThrow<SuspendMethodNotSupported> {
            fakeTask.suspendedMethod()
        }
    }

    "Should be able to dispatch method without parameter" {
        // when
        val fakeTask = client.newTask<FakeTask>()
        val deferred: Deferred<Unit> = client.async(fakeTask) { m1() }
        // then
        tagSlots.size shouldBe 0
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
            tags = setOf(),
            taskOptions = TaskOptions(),
            taskMeta = TaskMeta()
        )
    }

    "Should be able to dispatch method without parameter (Java syntax)" {
        // when
        val fakeTask = client.newTask(FakeTask::class.java)
        val deferred: Deferred<Unit> = client.async(fakeTask) { m1() }
        // then
        tagSlots.size shouldBe 0
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
            tags = setOf(),
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
        tagSlots.size shouldBe 0
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
            tags = setOf(),
            taskOptions = options,
            taskMeta = TaskMeta(meta)
        )
    }

    "Should be able to dispatch method with tags" {
        // when
        val fakeTask = client.newTask<FakeTask>(tags = setOf("foo", "bar"))
        val deferred: Deferred<Unit> = client.async(fakeTask) { m1() }
        // then
        tagSlots.size shouldBe 2
        tagSlots[0] shouldBe AddTaskTag(
            tag = Tag("foo"),
            name = TaskName(FakeTask::class.java.name),
            taskId = TaskId(deferred.id),
        )
        tagSlots[1] shouldBe AddTaskTag(
            tag = Tag("bar"),
            name = TaskName(FakeTask::class.java.name),
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
            tags = setOf(Tag("foo"), Tag("bar")),
            taskOptions = TaskOptions(),
            taskMeta = TaskMeta()
        )
    }

    "Should be able to dispatch a method with a primitive as parameter" {
        // when
        val fakeTask = client.newTask<FakeTask>()
        val deferred: Deferred<String> = client.async(fakeTask) { m1(0) }
        // then
        tagSlots.size shouldBe 0
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
            tags = setOf(),
            taskOptions = TaskOptions(),
            taskMeta = TaskMeta()
        )
    }

    "Should be able to dispatch a method with null as parameter" {
        // when
        val fakeTask = client.newTask<FakeTask>()
        val deferred = client.async(fakeTask) { m1(null) }
        // then
        tagSlots.size shouldBe 0
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
            tags = setOf(),
            taskOptions = TaskOptions(),
            taskMeta = TaskMeta()
        )
    }

    "Should be able to dispatch a method with multiple definition" {
        // when
        val fakeTask = client.newTask<FakeTask>()
        val deferred = client.async(fakeTask) { m1("a") }
        // then
        tagSlots.size shouldBe 0
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
            tags = setOf(),
            taskOptions = TaskOptions(),
            taskMeta = TaskMeta()
        )
    }

    "Should be able to dispatch a method with multiple parameters" {
        // when
        val fakeTask = client.newTask<FakeTask>()
        val deferred = client.async(fakeTask) { m1(0, "a") }
        // then
        tagSlots.size shouldBe 0
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
            tags = setOf(),
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
        tagSlots.size shouldBe 0
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
            tags = setOf(),
            taskOptions = TaskOptions(),
            taskMeta = TaskMeta()
        )
    }

    "Should be able to dispatch a method with a primitive as return value" {
        // when
        val fakeTask = client.newTask<FakeTask>()
        val deferred = client.async(fakeTask) { m2() }
        // then
        tagSlots.size shouldBe 0
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
            tags = setOf(),
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
        tagSlots.size shouldBe 0
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
            tags = setOf(),
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
        tagSlots.size shouldBe 0
        taskSlot.captured shouldBe CancelTask(
            taskId = TaskId(id),
            taskName = TaskName(FakeTask::class.java.name),
            taskReturnValue = MethodReturnValue.from(null)
        )
    }

    "Should be able to cancel task per id with output" {
        val output = TestFactory.random<String>()
        // when
        val id = UUID.randomUUID()
        val fakeTask = client.getTask<FakeTask>(id)
        client.cancel(fakeTask, output)
        // then
        tagSlots.size shouldBe 0
        taskSlot.captured shouldBe CancelTask(
            taskId = TaskId(id),
            taskName = TaskName(FakeTask::class.java.name),
            taskReturnValue = MethodReturnValue.from(output)
        )
    }

    "Should be able to cancel task per tag" {
        // when
        val fakeTask = client.getTask<FakeTask>("foo")
        client.cancel(fakeTask)
        // then
        tagSlots.size shouldBe 1
        tagSlots[0] shouldBe CancelTaskPerTag(
            tag = Tag("foo"),
            name = TaskName(FakeTask::class.java.name),
            taskReturnValue = MethodReturnValue.from(null)
        )
        taskSlot.isCaptured shouldBe false
    }

    "Should be able to cancel task per tag with output" {
        val output = TestFactory.random<String>()
        // when
        val fakeTask = client.getTask<FakeTask>("foo")
        client.cancel(fakeTask, output)
        // then
        tagSlots.size shouldBe 1
        tagSlots[0] shouldBe CancelTaskPerTag(
            tag = Tag("foo"),
            name = TaskName(FakeTask::class.java.name),
            taskReturnValue = MethodReturnValue.from(output)
        )
        taskSlot.isCaptured shouldBe false
    }

    "Should be able to cancel task just dispatched" {
        // when
        val fakeTask = client.newTask<FakeTask>()
        val deferred = client.async(fakeTask) { m1() }

        client.cancel(fakeTask)
        // then
        tagSlots.size shouldBe 0
        taskSlot.captured shouldBe CancelTask(
            taskId = TaskId(deferred.id),
            taskName = TaskName(FakeTask::class.java.name),
            taskReturnValue = MethodReturnValue.from(null)
        )
    }

    "Should be able to retry task per id" {
        // when
        val id = UUID.randomUUID()
        val fakeTask = client.getTask<FakeTask>(id)
        client.retry(fakeTask)
        // then
        tagSlots.size shouldBe 0
        taskSlot.captured shouldBe RetryTask(
            taskId = TaskId(id),
            taskName = TaskName(FakeTask::class.java.name),
            methodName = null,
            methodParameterTypes = null,
            methodParameters = null,
            tags = null,
            taskOptions = null,
            taskMeta = null
        )
    }

    "Should be able to retry task per tag" {
        // when
        val fakeTask = client.getTask<FakeTask>("foo")
        client.retry(fakeTask)
        // then
        tagSlots.size shouldBe 1
        tagSlots[0] shouldBe RetryTaskPerTag(
            tag = Tag("foo"),
            name = TaskName(FakeTask::class.java.name),
            methodName = null,
            methodParameterTypes = null,
            methodParameters = null,
            tags = null,
            taskOptions = null,
            taskMeta = null
        )
        taskSlot.isCaptured shouldBe false
    }

    "Should be able to retry a task just dispatched " {
        // when
        val fakeTask = client.newTask<FakeTask>()
        val deferred = client.async(fakeTask) { m1() }

        client.retry(fakeTask)
        // then
        tagSlots.size shouldBe 0
        taskSlot.captured shouldBe RetryTask(
            taskId = TaskId(deferred.id),
            taskName = TaskName(FakeTask::class.java.name),
            methodName = null,
            methodParameterTypes = null,
            methodParameters = null,
            tags = setOf(),
            taskOptions = TaskOptions(),
            taskMeta = TaskMeta()
        )
    }
})
