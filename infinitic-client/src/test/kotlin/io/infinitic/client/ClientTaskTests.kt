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
import io.infinitic.client.samples.FooTask
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
import io.infinitic.common.tasks.tags.messages.GetTaskIds
import io.infinitic.common.tasks.tags.messages.RetryTaskPerTag
import io.infinitic.common.tasks.tags.messages.TaskTagEngineMessage
import io.infinitic.common.workflows.engine.messages.WorkflowEngineMessage
import io.infinitic.common.workflows.tags.messages.WorkflowTagEngineMessage
import io.infinitic.exceptions.clients.CanNotApplyOnNewTaskStubException
import io.infinitic.exceptions.clients.NotAStubException
import io.infinitic.exceptions.clients.SuspendMethodNotSupportedException
import io.kotest.assertions.throwables.shouldNotThrow
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.mockk.slot
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import java.util.UUID
import java.util.concurrent.CopyOnWriteArrayList

private val taskTagSlots = CopyOnWriteArrayList<TaskTagEngineMessage>() // multithread update
private val workflowTagSlots = CopyOnWriteArrayList<WorkflowTagEngineMessage>() // multithread update
private val taskSlot = slot<TaskEngineMessage>()
private val workflowSlot = slot<WorkflowEngineMessage>()

class ClientTask : InfiniticClient() {
    override val sendingScope = CoroutineScope(Dispatchers.IO)
    override val clientName = ClientName("clientTest")
    override val sendToTaskTagEngine = mockSendToTaskTagEngine(this, taskTagSlots)
    override val sendToTaskEngine = mockSendToTaskEngine(this, taskSlot)
    override val sendToWorkflowTagEngine = mockSendToWorkflowTagEngine(this, workflowTagSlots)
    override val sendToWorkflowEngine = mockSendToWorkflowEngine(this, workflowSlot)
}

class ClientTaskTests : StringSpec({
    val client = ClientTask()

    beforeTest {
        taskTagSlots.clear()
        taskSlot.clear()
        workflowTagSlots.clear()
        workflowSlot.clear()
    }

    afterTest {
        client.join()
    }

    "Should throw when not using a stub" {
        shouldThrow<NotAStubException> {
            client.cancel("")
        }

        shouldThrow<NotAStubException> {
            client.complete("", null)
        }

        shouldThrow<NotAStubException> {
            client.retry("")
        }
    }

    "Should not throw when re-using a task stub" {
        // when
        val fakeTask = client.newTask<FakeTask>()
        shouldNotThrow<Throwable> {
            client.dispatch(fakeTask::m0)()
            client.dispatch(fakeTask::m0)()
        }
    }

    "Should throw when using new stub" {
        // when
        val fakeTask = client.newTask<FakeTask>()

        shouldThrow<CanNotApplyOnNewTaskStubException> {
            client.cancel(fakeTask)
        }

        shouldThrow<CanNotApplyOnNewTaskStubException> {
            client.complete(fakeTask, null)
        }

        shouldThrow<CanNotApplyOnNewTaskStubException> {
            client.retry(fakeTask)
        }
    }

    "Should throw when using a suspend method" {
        // when
        val fakeTask = client.newTask<FakeTask>()
        shouldThrow<SuspendMethodNotSupportedException> {
            fakeTask.suspendedMethod()
        }
    }

    "dispatch method without parameter" {
        // when
        val fakeTask = client.newTask<FakeTask>()
        val deferred: Deferred<Unit> = client.dispatch(fakeTask::m0)().join()
        // then
        taskTagSlots.size shouldBe 0
        taskSlot.captured shouldBe DispatchTask(
            clientName = client.clientName,
            clientWaiting = false,
            taskId = TaskId(deferred.id),
            taskName = TaskName(FakeTask::class.java.name),
            methodName = MethodName("m0"),
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

    "dispatch method with helper without parameter" {
        // when
        val deferred: Deferred<Unit> = client.dispatchTask(FakeTask::m0)().join()
        // then
        taskTagSlots.size shouldBe 0
        taskSlot.captured shouldBe DispatchTask(
            clientName = client.clientName,
            clientWaiting = false,
            taskId = TaskId(deferred.id),
            taskName = TaskName(FakeTask::class.java.name),
            methodName = MethodName("m0"),
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

    "dispatch method with annotations" {
        // when
        val fooTask = client.newTask<FooTask>()
        val deferred: Deferred<Unit> = client.dispatch(fooTask::m)().join()
        // then
        taskTagSlots.size shouldBe 0
        taskSlot.captured shouldBe DispatchTask(
            clientName = client.clientName,
            clientWaiting = false,
            taskId = TaskId(deferred.id),
            taskName = TaskName("foo"),
            methodName = MethodName("bar"),
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

    "dispatch method with annotations on parent" {
        // when
        val fooTask = client.newTask<FooTask>()
        val deferred: Deferred<String> = client.dispatch(fooTask::annotated)().join()
        // then
        taskTagSlots.size shouldBe 0
        taskSlot.captured shouldBe DispatchTask(
            clientName = client.clientName,
            clientWaiting = false,
            taskId = TaskId(deferred.id),
            taskName = TaskName("foo"),
            methodName = MethodName("bar"),
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

    "dispatch method without parameter (Java syntax)" {
        // when
        val fakeTask = client.newTask(FakeTask::class.java)
        val deferred: Deferred<Unit> = client.dispatch(fakeTask::m0)().join()
        // then
        taskTagSlots.size shouldBe 0
        taskSlot.captured shouldBe DispatchTask(
            clientName = client.clientName,
            clientWaiting = false,
            taskId = TaskId(deferred.id),
            taskName = TaskName(FakeTask::class.java.name),
            methodName = MethodName("m0"),
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

    "dispatch method with options and meta" {
        // when
        val options = TestFactory.random<TaskOptions>()
        val meta = mapOf(
            "foo" to TestFactory.random<ByteArray>(),
            "bar" to TestFactory.random<ByteArray>()
        )
        val fakeTask = client.newTask<FakeTask>(options = options, meta = meta)
        val deferred: Deferred<Unit> = client.dispatch(fakeTask::m0)().join()
        // then
        taskTagSlots.size shouldBe 0
        taskSlot.captured shouldBe DispatchTask(
            clientName = client.clientName,
            clientWaiting = false,
            taskId = TaskId(deferred.id),
            taskName = TaskName(FakeTask::class.java.name),
            methodName = MethodName("m0"),
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

    "dispatch method with tags" {
        // when
        val fakeTask = client.newTask<FakeTask>(tags = setOf("foo", "bar"))
        val deferred: Deferred<Unit> = client.dispatch(fakeTask::m0)().join()
        // then
        taskTagSlots.toSet() shouldBe setOf(
            AddTaskTag(
                taskTag = TaskTag("foo"),
                taskName = TaskName(FakeTask::class.java.name),
                taskId = TaskId(deferred.id),
            ),
            AddTaskTag(
                taskTag = TaskTag("bar"),
                taskName = TaskName(FakeTask::class.java.name),
                taskId = TaskId(deferred.id),
            )
        )
        taskSlot.captured shouldBe DispatchTask(
            clientName = client.clientName,
            clientWaiting = false,
            taskId = TaskId(deferred.id),
            taskName = TaskName(FakeTask::class.java.name),
            methodName = MethodName("m0"),
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

    "dispatch a method with a primitive as parameter" {
        // when
        val fakeTask = client.newTask<FakeTask>()
        val deferred: Deferred<String> = client.dispatch(fakeTask::m1)(0).join()
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

    "dispatch a method with null as parameter" {
        // when
        val fakeTask = client.newTask<FakeTask>()
        val deferred = client.dispatch(fakeTask::m2)(null).join()
        // then
        taskTagSlots.size shouldBe 0
        taskSlot.captured shouldBe DispatchTask(
            clientName = client.clientName,
            clientWaiting = false,
            taskId = TaskId(deferred.id),
            taskName = TaskName(FakeTask::class.java.name),
            methodName = MethodName("m2"),
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

    "dispatch a method with multiple definition" {
        // when
        val fakeTask = client.newTask<FakeTask>()
        val deferred = client.dispatch(fakeTask::m2)("a").join()
        // then
        taskTagSlots.size shouldBe 0
        taskSlot.captured shouldBe DispatchTask(
            clientName = client.clientName,
            clientWaiting = false,
            taskId = TaskId(deferred.id),
            taskName = TaskName(FakeTask::class.java.name),
            methodName = MethodName("m2"),
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

    "dispatch a method with multiple parameters" {
        // when
        val fakeTask = client.newTask<FakeTask>()
        val deferred = client.dispatch(fakeTask::m3)(0, "a").join()
        // then
        taskTagSlots.size shouldBe 0
        taskSlot.captured shouldBe DispatchTask(
            clientName = client.clientName,
            clientWaiting = false,
            taskId = TaskId(deferred.id),
            taskName = TaskName(FakeTask::class.java.name),
            methodName = MethodName("m3"),
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

    "dispatch a method with an interface as parameter" {
        // when
        val fakeTask = client.newTask<FakeTask>()
        val fake = FakeClass()
        val deferred = client.dispatch(fakeTask::m4)(fake).join()
        // then
        taskTagSlots.size shouldBe 0
        taskSlot.captured shouldBe DispatchTask(
            clientName = client.clientName,
            clientWaiting = false,
            taskId = TaskId(deferred.id),
            taskName = TaskName(FakeTask::class.java.name),
            methodName = MethodName("m4"),
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

    "dispatch a method with a primitive as return value" {
        // when
        val fakeTask = client.newTask<FakeTask>()
        val deferred = client.dispatch(fakeTask::m5)().join()
        // then
        taskTagSlots.size shouldBe 0
        taskSlot.captured shouldBe DispatchTask(
            clientName = client.clientName,
            clientWaiting = false,
            taskId = TaskId(deferred.id),
            taskName = TaskName(FakeTask::class.java.name),
            methodName = MethodName("m5"),
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

    "dispatch a method synchronously" {
        // when
        val fakeTask = client.newTask<FakeTask>()
        val result = fakeTask.m3(0, "a")
        // then
        result shouldBe "success"
        taskTagSlots.size shouldBe 0
        val msg = taskSlot.captured
        msg shouldBe DispatchTask(
            clientName = client.clientName,
            clientWaiting = true,
            taskId = msg.taskId,
            taskName = TaskName(FakeTask::class.java.name),
            methodName = MethodName("m3"),
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

    "dispatch a method from a parent interface" {
        // when
        val fakeTask = client.newTask<FakeTask>()
        val result = fakeTask.parent()
        // then
        result shouldBe "success"
        taskTagSlots.size shouldBe 0
        val msg = taskSlot.captured
        msg shouldBe DispatchTask(
            clientName = client.clientName,
            clientWaiting = true,
            taskId = msg.taskId,
            taskName = TaskName(FakeTask::class.java.name),
            methodName = MethodName("parent"),
            methodParameterTypes = MethodParameterTypes(listOf()),
            methodParameters = MethodParameters.from(),
            workflowId = null,
            workflowName = null,
            methodRunId = null,
            taskTags = setOf(),
            taskOptions = TaskOptions(),
            taskMeta = TaskMeta()
        )
    }

    "await for a task just dispatched" {
        // when
        val fakeTask = client.newTask<FakeTask>()
        val deferred = client.dispatch(fakeTask::m3)(0, "a").join()
        val result = deferred.await()
        // then
        result shouldBe "success"
    }

    "cancel task per id - syntax 1" {
        // when
        val id = UUID.randomUUID()
        val fakeTask = client.getTask<FakeTask>(id)
        client.cancel(fakeTask).join()
        // then
        taskTagSlots.size shouldBe 0
        taskSlot.captured shouldBe CancelTask(
            taskId = TaskId(id),
            taskName = TaskName(FakeTask::class.java.name)
        )
    }

    "cancel task per id - syntax 2" {
        // when
        val id = UUID.randomUUID()
        client.cancelTask(FakeTask::class.java, id).join()
        // then
        taskTagSlots.size shouldBe 0
        taskSlot.captured shouldBe CancelTask(
            taskId = TaskId(id),
            taskName = TaskName(FakeTask::class.java.name)
        )
    }

    "cancel task per id - syntax 3" {
        // when
        val id = UUID.randomUUID()
        client.cancelTask<FakeTask>(id).join()
        // then
        taskTagSlots.size shouldBe 0
        taskSlot.captured shouldBe CancelTask(
            taskId = TaskId(id),
            taskName = TaskName(FakeTask::class.java.name)
        )
    }

    "cancel task per tag - syntax 1" {
        // when
        val fakeTask = client.getTask<FakeTask>("foo")
        client.cancel(fakeTask).join()
        // then
        taskTagSlots.size shouldBe 1
        taskTagSlots[0] shouldBe CancelTaskPerTag(
            taskTag = TaskTag("foo"),
            taskName = TaskName(FakeTask::class.java.name)
        )
        taskSlot.isCaptured shouldBe false
    }

    "cancel task per tag - syntax 2" {
        // when
        client.cancelTask<FakeTask>("foo").join()
        // then
        taskTagSlots.size shouldBe 1
        taskTagSlots[0] shouldBe CancelTaskPerTag(
            taskTag = TaskTag("foo"),
            taskName = TaskName(FakeTask::class.java.name)
        )
        taskSlot.isCaptured shouldBe false
    }

    "cancel task per tag - syntax 3" {
        // when
        client.cancelTask(FakeTask::class.java, "foo").join()
        // then
        taskTagSlots.size shouldBe 1
        taskTagSlots[0] shouldBe CancelTaskPerTag(
            taskTag = TaskTag("foo"),
            taskName = TaskName(FakeTask::class.java.name)
        )
        taskSlot.isCaptured shouldBe false
    }

    "cancel task just dispatched" {
        // when
        val fakeTask = client.newTask<FakeTask>()
        val deferred = client.dispatch(fakeTask::m0)().join()
        deferred.cancel().join()
        // then
        taskTagSlots.size shouldBe 0
        taskSlot.captured shouldBe CancelTask(
            taskId = TaskId(deferred.id),
            taskName = TaskName(FakeTask::class.java.name)
        )
    }

    "retry task per id - syntax 1" {
        // when
        val id = UUID.randomUUID()
        val fakeTask = client.getTask<FakeTask>(id)
        client.retry(fakeTask).join()
        // then
        taskTagSlots.size shouldBe 0
        taskSlot.captured shouldBe RetryTask(
            taskId = TaskId(id),
            taskName = TaskName(FakeTask::class.java.name)
        )
    }

    "retry task per id - syntax 2" {
        // when
        val id = UUID.randomUUID()
        client.retryTask<FakeTask>(id).join()
        // then
        taskTagSlots.size shouldBe 0
        taskSlot.captured shouldBe RetryTask(
            taskId = TaskId(id),
            taskName = TaskName(FakeTask::class.java.name)
        )
    }

    "retry task per id - syntax 3" {
        // when
        val id = UUID.randomUUID()
        client.retryTask(FakeTask::class.java, id).join()
        // then
        taskTagSlots.size shouldBe 0
        taskSlot.captured shouldBe RetryTask(
            taskId = TaskId(id),
            taskName = TaskName(FakeTask::class.java.name)
        )
    }

    "retry a task just dispatched " {
        // when
        val fakeTask = client.newTask<FakeTask>()
        val deferred = client.dispatch(fakeTask::m0)().join()
        deferred.retry().join()
        // then
        taskTagSlots.size shouldBe 0
        taskSlot.captured shouldBe RetryTask(
            taskId = TaskId(deferred.id),
            taskName = TaskName(FakeTask::class.java.name)
        )
    }

    "retry task per tag - syntax 1" {
        // when
        val fakeTask = client.getTask<FakeTask>("foo")
        client.retry(fakeTask).join()
        // then
        taskTagSlots.size shouldBe 1
        taskTagSlots[0] shouldBe RetryTaskPerTag(
            taskTag = TaskTag("foo"),
            taskName = TaskName(FakeTask::class.java.name)
        )
        taskSlot.isCaptured shouldBe false
    }

    "retry task per tag - syntax 2" {
        // when
        client.retryTask<FakeTask>("foo").join()
        // then
        taskTagSlots.size shouldBe 1
        taskTagSlots[0] shouldBe RetryTaskPerTag(
            taskTag = TaskTag("foo"),
            taskName = TaskName(FakeTask::class.java.name)
        )
        taskSlot.isCaptured shouldBe false
    }

    "retry task per tag - syntax 3" {
        // when
        client.retryTask(FakeTask::class.java, "foo").join()
        // then
        taskTagSlots.size shouldBe 1
        taskTagSlots[0] shouldBe RetryTaskPerTag(
            taskTag = TaskTag("foo"),
            taskName = TaskName(FakeTask::class.java.name)
        )
        taskSlot.isCaptured shouldBe false
    }

    "get task ids par name and workflow" {
        val taskIds = client.getTaskIds<FakeTask>("foo")
        // then
        taskIds.size shouldBe 2
        taskTagSlots.size shouldBe 1
        taskTagSlots[0] shouldBe GetTaskIds(
            taskName = TaskName(FakeTask::class.java.name),
            taskTag = TaskTag("foo"),
            clientName = ClientName("clientTest")
        )
        taskSlot.isCaptured shouldBe false
    }
})
