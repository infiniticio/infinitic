package io.infinitic.client

import io.infinitic.client.samples.FakeClass
import io.infinitic.client.samples.FakeInterface
import io.infinitic.client.samples.FakeWorkflow
import io.infinitic.messaging.api.dispatcher.Dispatcher
import io.infinitic.common.tasks.messages.ForTaskEngineMessage
import io.infinitic.common.workflows.data.methodRuns.MethodInput
import io.infinitic.common.workflows.data.workflows.WorkflowMeta
import io.infinitic.common.workflows.data.methodRuns.MethodName
import io.infinitic.common.workflows.data.workflows.WorkflowName
import io.infinitic.common.workflows.data.workflows.WorkflowOptions
import io.infinitic.common.workflows.messages.DispatchWorkflow
import io.infinitic.common.workflows.messages.ForWorkflowEngineMessage
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot

class ClientWorkflowTests : StringSpec({
    val dispatcher = mockk<Dispatcher>()

    val taskSlot = slot<ForTaskEngineMessage>()
    coEvery { dispatcher.toTaskEngine(capture(taskSlot)) } just Runs

    val workflowSlot = slot<ForWorkflowEngineMessage>()
    coEvery { dispatcher.toWorkflowEngine(capture(workflowSlot)) } just Runs

    val client = Client(dispatcher)

    beforeTest {
        taskSlot.clear()
        workflowSlot.clear()
    }

    "Should be able to dispatch a workflow without parameter" {
        // when
        val workflow = client.dispatch(FakeWorkflow::class.java) { m1() }
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
        val workflow = client.dispatch(FakeWorkflow::class.java) { m1(0) }
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
        val workflow = client.dispatch(FakeWorkflow::class.java) { m1("a") }
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
        val workflow = client.dispatch(FakeWorkflow::class.java) { m1(0, "a") }
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
        val klass = FakeClass()
        val instance = client.dispatch(FakeWorkflow::class.java) { m1(klass) }
        // then
        workflowSlot.isCaptured shouldBe true
        val msg = workflowSlot.captured

        msg shouldBe DispatchWorkflow(
            workflowId = instance.workflowId,
            workflowName = WorkflowName(FakeWorkflow::class.java.name),
            methodName = MethodName("m1", listOf(FakeInterface::class.java.name)),
            methodInput = MethodInput(klass),
            workflowOptions = WorkflowOptions(),
            workflowMeta = WorkflowMeta()
        )
    }

    // TODO: add tests for options

    // TODO: add tests for meta

    // TODO: add tests for error cases
})
