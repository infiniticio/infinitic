package io.infinitic.workflowManager.client

import io.infinitic.common.data.interfaces.IdInterface
import io.infinitic.workflowManager.common.data.workflows.WorkflowId
import io.infinitic.workflowManager.common.data.workflows.WorkflowInput
import io.infinitic.workflowManager.common.data.workflows.WorkflowMeta
import io.infinitic.workflowManager.common.data.workflows.WorkflowName
import io.infinitic.workflowManager.common.data.workflows.WorkflowOptions
import io.infinitic.workflowManager.common.messages.DispatchWorkflow
import io.infinitic.workflowManager.common.messages.ForWorkflowEngineMessage
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot

class ClientTests : StringSpec({
    val dispatcher = mockk<WorkflowDispatcher>()
    val slot = slot<ForWorkflowEngineMessage>()
    every { dispatcher.toWorkflowEngine(capture(slot)) } just Runs
    val client = Client()
    client.workflowDispatcher = dispatcher

    beforeTest {
        slot.clear()
    }

    "Should be able to dispatch method without parameter" {
        // when
        val workflow = client.dispatchWorkflow<FakeWorkflow> { m1() }
        // then
        slot.isCaptured shouldBe true
        val msg = slot.captured
        msg shouldBe DispatchWorkflow(
            workflowId = workflow.workflowId,
            workflowInput = WorkflowInput(),
            workflowName = WorkflowName("${FakeWorkflow::class.java.name}::m1"),
            workflowOptions = WorkflowOptions(),
            workflowMeta = WorkflowMeta().withParameterTypes(listOf())
        )
    }

    "Should be able to dispatch a method with a primitive as parameter" {
        // when
        val workflow = client.dispatchWorkflow<FakeWorkflow> {
            m1(0)
        }
        // then
        slot.isCaptured shouldBe true
        val msg = slot.captured
        msg shouldBe DispatchWorkflow(
            workflowId = workflow.workflowId,
            workflowInput = WorkflowInput(0),
            workflowName = WorkflowName("${FakeWorkflow::class.java.name}::m1"),
            workflowOptions = WorkflowOptions(),
            workflowMeta = WorkflowMeta().withParameterTypes(listOf(Int::class.java.name))
        )
    }

    "Should be able to dispatch a method with multiple definition" {
        // when
        val workflow = client.dispatchWorkflow<FakeWorkflow> { m1("a") }
        // then
        slot.isCaptured shouldBe true
        val msg = slot.captured
        msg shouldBe DispatchWorkflow(
            workflowId = workflow.workflowId,
            workflowInput = WorkflowInput("a"),
            workflowName = WorkflowName("${FakeWorkflow::class.java.name}::m1"),
            workflowOptions = WorkflowOptions(),
            workflowMeta = WorkflowMeta().withParameterTypes(listOf(String::class.java.name))
        )
    }

    "Should be able to dispatch a method with multiple parameters" {
        // when
        val workflow = client.dispatchWorkflow<FakeWorkflow> { m1(0, "a") }
        // then
        slot.isCaptured shouldBe true
        val msg = slot.captured
        msg shouldBe DispatchWorkflow(
            workflowId = workflow.workflowId,
            workflowInput = WorkflowInput(0, "a"),
            workflowName = WorkflowName("${FakeWorkflow::class.java.name}::m1"),
            workflowOptions = WorkflowOptions(),
            workflowMeta = WorkflowMeta().withParameterTypes(listOf(Int::class.java.name, String::class.java.name))
        )
    }

    "Should be able to dispatch a method with an interface as parameter" {
        // when
        val workflowId = WorkflowId()
        val workflow = client.dispatchWorkflow<FakeWorkflow> { m1(workflowId) }
        // then
        slot.isCaptured shouldBe true
        val msg = slot.captured

        msg shouldBe DispatchWorkflow(
            workflowId = workflow.workflowId,
            workflowInput = WorkflowInput(workflowId),
            workflowName = WorkflowName("${FakeWorkflow::class.java.name}::m1"),
            workflowOptions = WorkflowOptions(),
            workflowMeta = WorkflowMeta().withParameterTypes(listOf(IdInterface::class.java.name))
        )
    }

    // TODO: add tests for cancel method

    // TODO: add tests for retry method

    // TODO: add tests for options

    // TODO: add tests for meta

    // TODO: add tests for error cases
})

private interface FakeWorkflow {
    fun m1()
    fun m1(i: Int): String
    fun m1(str: String): Any?
    fun m1(p1: Int, p2: String): String
    fun m1(id: IdInterface): String
}
