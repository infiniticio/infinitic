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

import io.infinitic.client.deferred.DeferredTask
import io.infinitic.client.samples.FakeClass
import io.infinitic.client.samples.FakeInterface
import io.infinitic.client.samples.FakeTask
import io.infinitic.client.samples.FooTask
import io.infinitic.common.clients.Deferred
import io.infinitic.common.data.ClientName
import io.infinitic.common.data.methods.MethodName
import io.infinitic.common.data.methods.MethodParameterTypes
import io.infinitic.common.data.methods.MethodParameters
import io.infinitic.common.fixtures.TestFactory
import io.infinitic.common.tasks.data.TaskId
import io.infinitic.common.tasks.data.TaskMeta
import io.infinitic.common.tasks.data.TaskName
import io.infinitic.common.tasks.data.TaskTag
import io.infinitic.common.tasks.engines.messages.DispatchTask
import io.infinitic.common.tasks.engines.messages.TaskEngineMessage
import io.infinitic.common.tasks.tags.messages.AddTagToTask
import io.infinitic.common.tasks.tags.messages.TaskTagMessage
import io.infinitic.common.workflows.engine.messages.WorkflowEngineMessage
import io.infinitic.common.workflows.tags.messages.WorkflowTagMessage
import io.infinitic.tasks.TaskOptions
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.mockk.slot
import java.util.concurrent.CopyOnWriteArrayList

private val taskTagSlots = CopyOnWriteArrayList<TaskTagMessage>() // multithread update
private val workflowTagSlots = CopyOnWriteArrayList<WorkflowTagMessage>() // multithread update
private val taskSlot = slot<TaskEngineMessage>()
private val workflowSlot = slot<WorkflowEngineMessage>()
private val clientNameTest = ClientName("clientTest")

class ClientTask : AbstractInfiniticClient() {
    override val clientName = clientNameTest
    override val sendToTaskTagEngine = mockSendToTaskTagEngine(this, taskTagSlots, clientName, sendingScope)
    override val sendToTaskEngine = mockSendToTaskEngine(this, taskSlot, clientName, sendingScope)
    override val sendToWorkflowTagEngine = mockSendToWorkflowTagEngine(this, workflowTagSlots, clientName, sendingScope)
    override val sendToWorkflowEngine = mockSendToWorkflowEngine(this, workflowSlot, clientName, sendingScope)
}

class ClientTaskTests : StringSpec({
    val client = ClientTask()

    val fakeTask = client.newTask(FakeTask::class.java)
    val fooTask = client.newTask(FooTask::class.java)

    val options = TestFactory.random<TaskOptions>()
    val meta: Map<String, ByteArray> = mapOf(
        "foo" to TestFactory.random(),
        "bar" to TestFactory.random()
    )
    val fakeTaskWithOptions = client.newTask(FakeTask::class.java, options = options)
    val fakeTaskWithMeta = client.newTask(FakeTask::class.java, meta = meta)
    val fakeTaskWithTags = client.newTask(FakeTask::class.java, tags = setOf("foo", "bar"))

    beforeTest {
        taskTagSlots.clear()
        taskSlot.clear()
        workflowTagSlots.clear()
        workflowSlot.clear()
    }

    afterTest {
        client.join()
    }

    "First call to getLastSyncDeferred() should be null" {
        client.lastDeferred shouldBe null
    }

    "Call to getLastSyncDeferred() should be provided last deferred" {
        fooTask.annotated()

        client.lastDeferred!!::class shouldBe DeferredTask::class
    }

    "Dispatch method without parameter" {
        // when
        val deferred: Deferred<Unit> = client.dispatch(fakeTask::m0)
        // then
        taskTagSlots.size shouldBe 0
        taskSlot.captured shouldBe DispatchTask(
            taskName = TaskName(FakeTask::class.java.name),
            taskId = TaskId(deferred.id),
            taskOptions = TaskOptions(),
            clientWaiting = false,
            methodName = MethodName("m0"),
            methodParameterTypes = MethodParameterTypes(listOf()),
            methodParameters = MethodParameters(),
            workflowId = null,
            workflowName = null,
            methodRunId = null,
            taskTags = setOf(),
            taskMeta = TaskMeta(),
            emitterName = clientNameTest
        )
    }

    "Dispatch method with annotations" {
        // when
        val deferred: Deferred<Unit> = client.dispatch(fooTask::m)
        // then
        taskTagSlots.size shouldBe 0
        taskSlot.captured shouldBe DispatchTask(
            taskName = TaskName("foo"),
            taskId = TaskId(deferred.id),
            taskOptions = TaskOptions(),
            clientWaiting = false,
            methodName = MethodName("bar"),
            methodParameterTypes = MethodParameterTypes(listOf()),
            methodParameters = MethodParameters(),
            workflowId = null,
            workflowName = null,
            methodRunId = null,
            taskTags = setOf(),
            taskMeta = TaskMeta(),
            emitterName = clientNameTest
        )
    }

    "Dispatch method with annotations on super" {
        // when
        val deferred: Deferred<String> = client.dispatch(fooTask::annotated)
        // then
        taskTagSlots.size shouldBe 0
        taskSlot.captured shouldBe DispatchTask(
            taskName = TaskName("foo"),
            taskId = TaskId(deferred.id),
            taskOptions = TaskOptions(),
            clientWaiting = false,
            methodName = MethodName("bar"),
            methodParameterTypes = MethodParameterTypes(listOf()),
            methodParameters = MethodParameters(),
            workflowId = null,
            workflowName = null,
            methodRunId = null,
            taskTags = setOf(),
            taskMeta = TaskMeta(),
            emitterName = clientNameTest
        )
    }

    "Dispatch method without parameter (Java syntax)" {
        // when
        val deferred: Deferred<Unit> = client.dispatch(fakeTask::m0)
        // then
        taskTagSlots.size shouldBe 0
        taskSlot.captured shouldBe DispatchTask(
            taskName = TaskName(FakeTask::class.java.name),
            taskId = TaskId(deferred.id),
            taskOptions = TaskOptions(),
            clientWaiting = false,
            methodName = MethodName("m0"),
            methodParameterTypes = MethodParameterTypes(listOf()),
            methodParameters = MethodParameters(),
            workflowId = null,
            workflowName = null,
            methodRunId = null,
            taskTags = setOf(),
            taskMeta = TaskMeta(),
            emitterName = clientNameTest
        )
    }

    "Dispatch method with options" {
        // when
        val deferred: Deferred<Unit> = client.dispatch(fakeTaskWithOptions::m0)
        // then
        taskTagSlots.size shouldBe 0
        taskSlot.captured shouldBe DispatchTask(
            taskName = TaskName(FakeTask::class.java.name),
            taskId = TaskId(deferred.id),
            taskOptions = options,
            clientWaiting = false,
            methodName = MethodName("m0"),
            methodParameterTypes = MethodParameterTypes(listOf()),
            methodParameters = MethodParameters(),
            workflowId = null,
            workflowName = null,
            methodRunId = null,
            taskTags = setOf(),
            taskMeta = TaskMeta(),
            emitterName = clientNameTest
        )
    }

    "Dispatch method with meta" {
        // when
        val deferred: Deferred<Unit> = client.dispatch(fakeTaskWithMeta::m0)
        // then
        taskTagSlots.size shouldBe 0
        taskSlot.captured shouldBe DispatchTask(
            taskName = TaskName(FakeTask::class.java.name),
            taskId = TaskId(deferred.id),
            taskOptions = TaskOptions(),
            clientWaiting = false,
            methodName = MethodName("m0"),
            methodParameterTypes = MethodParameterTypes(listOf()),
            methodParameters = MethodParameters(),
            workflowId = null,
            workflowName = null,
            methodRunId = null,
            taskTags = setOf(),
            taskMeta = TaskMeta(meta),
            emitterName = clientNameTest
        )
    }

    "Dispatch method with tags" {
        // when
        val deferred = client.dispatch(fakeTaskWithTags::m0)
        // then
        taskTagSlots.toSet() shouldBe setOf(
            AddTagToTask(
                taskName = TaskName(FakeTask::class.java.name),
                taskTag = TaskTag("foo"),
                taskId = TaskId(deferred.id),
                emitterName = clientNameTest,
            ),
            AddTagToTask(
                taskName = TaskName(FakeTask::class.java.name),
                taskTag = TaskTag("bar"),
                taskId = TaskId(deferred.id),
                emitterName = clientNameTest,
            )
        )
        taskSlot.captured shouldBe DispatchTask(
            taskName = TaskName(FakeTask::class.java.name),
            taskId = TaskId(deferred.id),
            taskOptions = TaskOptions(),
            clientWaiting = false,
            methodName = MethodName("m0"),
            methodParameterTypes = MethodParameterTypes(listOf()),
            methodParameters = MethodParameters(),
            workflowId = null,
            workflowName = null,
            methodRunId = null,
            taskTags = setOf(TaskTag("foo"), TaskTag("bar")),
            taskMeta = TaskMeta(),
            emitterName = clientNameTest
        )
    }

    "Second method should not overwrite the first one" {
        // when
        client.dispatch(fakeTask::m0)

        val deferred = client.dispatch(fakeTask::m1, 0)
        // then
        taskTagSlots.size shouldBe 0
        taskSlot.captured shouldBe DispatchTask(
            taskName = TaskName(FakeTask::class.java.name),
            taskId = TaskId(deferred.id),
            taskOptions = TaskOptions(),
            clientWaiting = false,
            methodName = MethodName("m1"),
            methodParameterTypes = MethodParameterTypes(listOf(Int::class.java.name)),
            methodParameters = MethodParameters.from(0),
            workflowId = null,
            workflowName = null,
            methodRunId = null,
            taskTags = setOf(),
            taskMeta = TaskMeta(),
            emitterName = clientNameTest
        )
    }

    "Dispatch a method with a primitive as parameter" {
        // when
        val deferred: Deferred<String> = client.dispatch(fakeTask::m1, 0)
        // then
        taskTagSlots.size shouldBe 0
        taskSlot.captured shouldBe DispatchTask(
            taskName = TaskName(FakeTask::class.java.name),
            taskId = TaskId(deferred.id),
            taskOptions = TaskOptions(),
            clientWaiting = false,
            methodName = MethodName("m1"),
            methodParameterTypes = MethodParameterTypes(listOf(Int::class.java.name)),
            methodParameters = MethodParameters.from(0),
            workflowId = null,
            workflowName = null,
            methodRunId = null,
            taskTags = setOf(),
            taskMeta = TaskMeta(),
            emitterName = clientNameTest
        )
    }

    "Dispatch a method with null as parameter" {
        // when
        val deferred = client.dispatch(fakeTask::m2, null)
        // then
        taskTagSlots.size shouldBe 0
        taskSlot.captured shouldBe DispatchTask(
            taskName = TaskName(FakeTask::class.java.name),
            taskId = TaskId(deferred.id),
            taskOptions = TaskOptions(),
            clientWaiting = false,
            methodName = MethodName("m2"),
            methodParameterTypes = MethodParameterTypes(listOf(String::class.java.name)),
            methodParameters = MethodParameters.from(null),
            workflowId = null,
            workflowName = null,
            methodRunId = null,
            taskTags = setOf(),
            taskMeta = TaskMeta(),
            emitterName = clientNameTest
        )
    }

    "Dispatch a method with multiple definition" {
        // when
        val deferred = client.dispatch(fakeTask::m2, "a")
        // then
        taskTagSlots.size shouldBe 0
        taskSlot.captured shouldBe DispatchTask(
            taskName = TaskName(FakeTask::class.java.name),
            taskId = TaskId(deferred.id),
            taskOptions = TaskOptions(),
            clientWaiting = false,
            methodName = MethodName("m2"),
            methodParameterTypes = MethodParameterTypes(listOf(String::class.java.name)),
            methodParameters = MethodParameters.from("a"),
            workflowId = null,
            workflowName = null,
            methodRunId = null,
            taskTags = setOf(),
            taskMeta = TaskMeta(),
            emitterName = clientNameTest
        )
    }

    "Dispatch a method with multiple parameters" {
        // when
        val deferred = client.dispatch(fakeTask::m3, 0, "a")
        // then
        taskTagSlots.size shouldBe 0
        taskSlot.captured shouldBe DispatchTask(
            taskName = TaskName(FakeTask::class.java.name),
            taskId = TaskId(deferred.id),
            taskOptions = TaskOptions(),
            clientWaiting = false,
            methodName = MethodName("m3"),
            methodParameterTypes = MethodParameterTypes(listOf(Int::class.java.name, String::class.java.name)),
//            methodParameters = MethodParameters(listOf(SerializedData.from(0), SerializedData.from("a"))),
            methodParameters = MethodParameters.from(0, "a"),
            workflowId = null,
            workflowName = null,
            methodRunId = null,
            taskTags = setOf(),
            taskMeta = TaskMeta(),
            emitterName = clientNameTest
        )
    }

    "Dispatch a method with an interface as parameter" {
        // when
        val fake = FakeClass()
        val deferred = client.dispatch(fakeTask::m4, fake)
        // then
        taskTagSlots.size shouldBe 0
        taskSlot.captured shouldBe DispatchTask(
            taskName = TaskName(FakeTask::class.java.name),
            taskId = TaskId(deferred.id),
            taskOptions = TaskOptions(),
            clientWaiting = false,
            methodName = MethodName("m4"),
            methodParameterTypes = MethodParameterTypes(listOf(FakeInterface::class.java.name)),
            methodParameters = MethodParameters.from(fake),
            workflowId = null,
            workflowName = null,
            methodRunId = null,
            taskTags = setOf(),
            taskMeta = TaskMeta(),
            emitterName = clientNameTest
        )
    }

    "Dispatch a method with a primitive as return value" {
        // when
        val deferred = client.dispatch(fakeTask::m5)
        // then
        taskTagSlots.size shouldBe 0
        taskSlot.captured shouldBe DispatchTask(
            taskName = TaskName(FakeTask::class.java.name),
            taskId = TaskId(deferred.id),
            taskOptions = TaskOptions(),
            clientWaiting = false,
            methodName = MethodName("m5"),
            methodParameterTypes = MethodParameterTypes(listOf()),
            methodParameters = MethodParameters(),
            workflowId = null,
            workflowName = null,
            methodRunId = null,
            taskTags = setOf(),
            taskMeta = TaskMeta(),
            emitterName = clientNameTest
        )
    }

    "Dispatch a method from a parent interface" {
        // when
        val result = fakeTask.parent()
        // then
        result shouldBe "success"
        taskTagSlots.size shouldBe 0
        val msg = taskSlot.captured
        msg shouldBe DispatchTask(
            taskName = TaskName(FakeTask::class.java.name),
            taskId = msg.taskId,
            taskOptions = TaskOptions(),
            clientWaiting = true,
            methodName = MethodName("parent"),
            methodParameterTypes = MethodParameterTypes(listOf()),
            methodParameters = MethodParameters.from(),
            workflowId = null,
            workflowName = null,
            methodRunId = null,
            taskTags = setOf(),
            taskMeta = TaskMeta(),
            emitterName = clientNameTest
        )
    }

    "wait fAor a task just dispatched" {
        client.dispatch(fakeTask::m3, 0, "a").await() shouldBe "success"
    }

//    "Cancel task per id (sync)" {
//        // when
//
//        val id = UUID.randomUUID().toString()
//        val task = client.getTaskById(FakeTask::class.java, id)
//
//        client.cancel(task)
//        // then
//        taskTagSlots.size shouldBe 0
//        taskSlot.captured shouldBe CancelTask(
//            taskName = TaskName(FakeTask::class.java.name),
//            taskId = TaskId(id),
//            emitterName = clientNameTest
//        )
//    }
//
//    "Cancel task per id (async)" {
//        // when
//        val id = UUID.randomUUID().toString()
//        val task = client.getTaskById(FakeTask::class.java, id)
//
//        client.cancelAsync(task).join()
//        // then
//        taskTagSlots.size shouldBe 0
//        taskSlot.captured shouldBe CancelTask(
//            taskName = TaskName(FakeTask::class.java.name),
//            taskId = TaskId(id),
//            emitterName = clientNameTest
//        )
//    }
//
//    "Cancel task per tag (sync)" {
//        // when
//        val task = client.getTaskByTag(FakeTask::class.java, "foo")
//        client.cancel(task)
//        // then
//        taskTagSlots.size shouldBe 1
//        taskTagSlots[0] shouldBe CancelTaskByTag(
//            taskName = TaskName(FakeTask::class.java.name),
//            taskTag = TaskTag("foo"),
//            emitterName = clientNameTest
//        )
//        taskSlot.isCaptured shouldBe false
//    }
//
//    "Cancel task per tag (async)" {
//        // when
//        val task = client.getTaskByTag(FakeTask::class.java, "foo")
//        client.cancelAsync(task).join()
//        // then
//        taskTagSlots.size shouldBe 1
//        taskTagSlots[0] shouldBe CancelTaskByTag(
//            taskName = TaskName(FakeTask::class.java.name),
//            taskTag = TaskTag("foo"),
//            emitterName = clientNameTest
//        )
//        taskSlot.isCaptured shouldBe false
//    }
//
//    "Cancel task just dispatched (sync)" {
//        // when
//        val deferred = client.dispatch(fakeTask::m0)
//        deferred.cancel()
//        // then
//        taskTagSlots.size shouldBe 0
//        taskSlot.captured shouldBe CancelTask(
//            taskName = TaskName(FakeTask::class.java.name),
//            taskId = TaskId(deferred.id),
//            emitterName = clientNameTest
//        )
//    }
//
//    "Cancel task just dispatched (async)" {
//        // when
//        val deferred = client.dispatch(fakeTask::m0)
//        deferred.cancelAsync().join()
//        // then
//        taskTagSlots.size shouldBe 0
//        taskSlot.captured shouldBe CancelTask(
//            taskName = TaskName(FakeTask::class.java.name),
//            taskId = TaskId(deferred.id),
//            emitterName = clientNameTest
//        )
//    }
//
//    "Retry task per id (sync)" {
//        // when
//        val id = UUID.randomUUID().toString()
//        val task = client.getTaskById(FakeTask::class.java, id)
//        client.retry(task)
//
//        // then
//        taskTagSlots.size shouldBe 0
//        taskSlot.captured shouldBe RetryTask(
//            taskName = TaskName(FakeTask::class.java.name),
//            taskId = TaskId(id),
//            emitterName = clientNameTest
//        )
//    }
//
//    "Retry task per id (async)" {
//        // when
//        val id = UUID.randomUUID().toString()
//        val task = client.getTaskById(FakeTask::class.java, id)
//        client.retryAsync(task).join()
//        // then
//        taskTagSlots.size shouldBe 0
//        taskSlot.captured shouldBe RetryTask(
//            taskName = TaskName(FakeTask::class.java.name),
//            taskId = TaskId(id),
//            emitterName = clientNameTest
//        )
//    }
//
//    "Retry a task just dispatched " {
//        // when
//        val deferred = client.dispatch(fakeTask::m0)
//        deferred.retry()
//        // then
//        taskTagSlots.size shouldBe 0
//        taskSlot.captured shouldBe RetryTask(
//            taskName = TaskName(FakeTask::class.java.name),
//            taskId = TaskId(deferred.id),
//            emitterName = clientNameTest
//        )
//    }
//
//    "Retry task per tag (sync)" {
//        // when
//        val task = client.getTaskByTag(FakeTask::class.java, "foo")
//        client.retry(task)
//        // then
//        taskTagSlots.size shouldBe 1
//        taskTagSlots[0] shouldBe RetryTaskByTag(
//            taskName = TaskName(FakeTask::class.java.name),
//            taskTag = TaskTag("foo"),
//            emitterName = clientNameTest
//        )
//        taskSlot.isCaptured shouldBe false
//    }
//
//    "Retry task per tag (async)" {
//        // when
//        val task = client.getTaskByTag(FakeTask::class.java, "foo")
//        client.retryAsync(task).join()
//        // then
//        taskTagSlots.size shouldBe 1
//        taskTagSlots[0] shouldBe RetryTaskByTag(
//            taskName = TaskName(FakeTask::class.java.name),
//            taskTag = TaskTag("foo"),
//            emitterName = clientNameTest
//        )
//        taskSlot.isCaptured shouldBe false
//    }
//
//    "Get task ids par name and workflow" {
//        val task = client.getTaskByTag(FakeTask::class.java, "foo")
//        val taskIds = client.getIds(task)
//        // then
//        taskIds.size shouldBe 2
//        taskTagSlots.size shouldBe 1
//        taskTagSlots[0] shouldBe GetTaskIdsByTag(
//            taskName = TaskName(FakeTask::class.java.name),
//            taskTag = TaskTag("foo"),
//            emitterName = clientNameTest,
//        )
//        taskSlot.isCaptured shouldBe false
//    }
})
