package io.infinitic.workflowManager.client

import io.infinitic.messaging.api.dispatcher.Dispatcher
import io.infinitic.taskManager.common.data.TaskId
import io.infinitic.taskManager.common.data.TaskInput
import io.infinitic.taskManager.common.data.TaskMeta
import io.infinitic.taskManager.common.data.TaskName
import io.infinitic.taskManager.common.data.TaskOptions
import io.infinitic.taskManager.common.messages.DispatchTask
import io.infinitic.taskManager.common.messages.ForTaskEngineMessage
import io.infinitic.workflowManager.common.data.workflows.WorkflowId
import io.infinitic.workflowManager.common.data.methodRuns.MethodInput
import io.infinitic.workflowManager.common.data.workflows.WorkflowMeta
import io.infinitic.workflowManager.common.data.methodRuns.MethodName
import io.infinitic.workflowManager.common.data.workflows.WorkflowName
import io.infinitic.workflowManager.common.data.workflows.WorkflowOptions
import io.infinitic.workflowManager.common.messages.DispatchWorkflow
import io.infinitic.workflowManager.common.messages.ForWorkflowEngineMessage
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot

class ClientTests : StringSpec({
    val taskDispatcher = mockk<Dispatcher>()
    val taskSlot = slot<ForTaskEngineMessage>()
    coEvery { taskDispatcher.toTaskEngine(capture(taskSlot)) } just Runs

    val workflowDispatcher = mockk<WorkflowDispatcher>()
    val workflowSlot = slot<ForWorkflowEngineMessage>()
    coEvery { workflowDispatcher.toWorkflowEngine(capture(workflowSlot)) } just Runs

    val client = Client(taskDispatcher, workflowDispatcher)

    beforeTest {
        taskSlot.clear()
        workflowSlot.clear()
    }

    "Should be able to dispatch a workflow without parameter" {
        // when
        val workflow = client.dispatchWorkflow<FakeWorkflow> { m1() }
        // then
        workflowSlot.isCaptured shouldBe true
        val msg = workflowSlot.captured
        msg shouldBe DispatchWorkflow(
            workflowId = workflow.workflowId,
            workflowName = WorkflowName(FakeWorkflow::class.java.name),
            methodName = MethodName("m1", listOf()),
            methodInput = MethodInput(),
            workflowOptions = WorkflowOptions(),
            workflowMeta = WorkflowMeta()
        )
    }

    "Should be able to dispatch a workflow with a primitive as parameter" {
        // when
        val workflow = client.dispatchWorkflow<FakeWorkflow> {
            m1(0)
        }
        // then
        workflowSlot.isCaptured shouldBe true
        val msg = workflowSlot.captured
        msg shouldBe DispatchWorkflow(
            workflowId = workflow.workflowId,
            workflowName = WorkflowName(FakeWorkflow::class.java.name),
            methodName = MethodName("m1", listOf(Integer::class.java.name)),
            methodInput = MethodInput(0),
            workflowOptions = WorkflowOptions(),
            workflowMeta = WorkflowMeta()
        )
    }

    "Should be able to dispatch a workflow with multiple method definition" {
        // when
        val workflow = client.dispatchWorkflow<FakeWorkflow> { m1("a") }
        // then
        workflowSlot.isCaptured shouldBe true
        val msg = workflowSlot.captured
        msg shouldBe DispatchWorkflow(
            workflowId = workflow.workflowId,
            workflowName = WorkflowName(FakeWorkflow::class.java.name),
            methodName = MethodName("m1", listOf(String::class.java.name)),
            methodInput = MethodInput("a"),
            workflowOptions = WorkflowOptions(),
            workflowMeta = WorkflowMeta()
        )
    }

    "Should be able to dispatch a workflow with multiple parameters" {
        // when
        val workflow = client.dispatchWorkflow<FakeWorkflow> { m1(0, "a") }
        // then
        workflowSlot.isCaptured shouldBe true
        val msg = workflowSlot.captured
        msg shouldBe DispatchWorkflow(
            workflowId = workflow.workflowId,
            workflowName = WorkflowName(FakeWorkflow::class.java.name),
            methodName = MethodName("m1", listOf(Int::class.java.name, String::class.java.name)),
            methodInput = MethodInput(0, "a"),
            workflowOptions = WorkflowOptions(),
            workflowMeta = WorkflowMeta()
        )
    }

    "Should be able to dispatch a workflow with an interface as parameter" {
        // when
        val workflowId = WorkflowId()
        val instance = client.dispatchWorkflow<FakeWorkflow> { m1(workflowId) }
        // then
        workflowSlot.isCaptured shouldBe true
        val msg = workflowSlot.captured

        msg shouldBe DispatchWorkflow(
            workflowId = instance.workflowId,
            workflowName = WorkflowName(FakeWorkflow::class.java.name),
            methodName = MethodName("m1", listOf(CharSequence::class.java.name)),
            methodInput = MethodInput(workflowId),
            workflowOptions = WorkflowOptions(),
            workflowMeta = WorkflowMeta()
        )
    }

    "Should be able to dispatch a task without parameter" {
        // when
        val task = client.dispatchTask<FakeTask> { m1() }
        // then
        taskSlot.isCaptured shouldBe true
        val msg = taskSlot.captured
        msg shouldBe DispatchTask(
            taskId = task.taskId,
            taskInput = TaskInput(),
            taskName = TaskName("${FakeTask::class.java.name}::m1"),
            taskOptions = TaskOptions(),
            taskMeta = TaskMeta().withParameterTypes(listOf())
        )
    }

    "Should be able to dispatch a task with a primitive as parameter" {
        // when
        val task = client.dispatchTask<FakeTask> {
            m1(0)
        }
        // then
        taskSlot.isCaptured shouldBe true
        val msg = taskSlot.captured
        msg shouldBe DispatchTask(
            taskId = task.taskId,
            taskInput = TaskInput(0),
            taskName = TaskName("${FakeTask::class.java.name}::m1"),
            taskOptions = TaskOptions(),
            taskMeta = TaskMeta().withParameterTypes(listOf(Integer::class.java.name))
        )
    }

    "Should be able to dispatch a task with null as parameter" {
        // when
        val task = client.dispatchTask<FakeTask> {
            m1(null)
        }
        // then
        taskSlot.isCaptured shouldBe true
        val msg = taskSlot.captured
        msg shouldBe DispatchTask(
            taskId = task.taskId,
            taskInput = TaskInput(null),
            taskName = TaskName("${FakeTask::class.java.name}::m1"),
            taskOptions = TaskOptions(),
            taskMeta = TaskMeta().withParameterTypes(listOf(Integer::class.java.name))
        )
    }

    "Should be able to dispatch a task with multiple definition" {
        // when
        val task = client.dispatchTask<FakeTask> { m1("a") }
        // then
        taskSlot.isCaptured shouldBe true
        val msg = taskSlot.captured
        msg shouldBe DispatchTask(
            taskId = task.taskId,
            taskInput = TaskInput("a"),
            taskName = TaskName("${FakeTask::class.java.name}::m1"),
            taskOptions = TaskOptions(),
            taskMeta = TaskMeta().withParameterTypes(listOf(String::class.java.name))
        )
    }

    "Should be able to dispatch a task with multiple parameters" {
        // when
        val task = client.dispatchTask<FakeTask> { m1(0, "a") }
        // then
        taskSlot.isCaptured shouldBe true
        val msg = taskSlot.captured
        msg shouldBe DispatchTask(
            taskId = task.taskId,
            taskInput = TaskInput(0, "a"),
            taskName = TaskName("${FakeTask::class.java.name}::m1"),
            taskOptions = TaskOptions(),
            taskMeta = TaskMeta().withParameterTypes(listOf(Int::class.java.name, String::class.java.name))
        )
    }

    "Should be able to dispatch a task with an interface as parameter" {
        // when
        val taskId = TaskId()
        val task = client.dispatchTask<FakeTask> { m1(taskId) }
        // then
        taskSlot.isCaptured shouldBe true
        val msg = taskSlot.captured

        msg shouldBe DispatchTask(
            taskId = task.taskId,
            taskInput = TaskInput(taskId),
            taskName = TaskName("${FakeTask::class.java.name}::m1"),
            taskOptions = TaskOptions(),
            taskMeta = TaskMeta().withParameterTypes(listOf(CharSequence::class.java.name))
        )
    }

    // TODO: add tests for options

    // TODO: add tests for meta

    // TODO: add tests for error cases
})

private interface FakeTask {
    fun m1()
    fun m1(i: Int?): String
    fun m1(str: String): Any?
    fun m1(p1: Int, p2: String): String
    fun m1(id: CharSequence): String
}

private interface FakeWorkflow {
    fun m1()
    fun m1(i: Int?): String
    fun m1(str: String): Any?
    fun m1(p1: Int, p2: String): String
    fun m1(id: CharSequence): String
}
