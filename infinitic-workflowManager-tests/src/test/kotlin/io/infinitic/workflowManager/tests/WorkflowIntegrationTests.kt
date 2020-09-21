package io.infinitic.workflowManager.tests

import io.infinitic.taskManager.data.AvroTaskStatus
import io.infinitic.taskManager.tests.inMemory.InMemoryDispatcherTest
import io.infinitic.taskManager.tests.inMemory.InMemoryStorageTest
import io.infinitic.taskManager.worker.Worker
import io.infinitic.workflowManager.common.data.workflows.WorkflowInstance
import io.infinitic.workflowManager.common.data.workflowTasks.WorkflowTask
import io.infinitic.workflowManager.worker.workflowTasks.WorkflowTaskImpl
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldBeIn
import io.kotest.matchers.shouldBe
import io.mockk.mockk
import kotlinx.coroutines.coroutineScope
import org.slf4j.Logger

private val mockLogger = mockk<Logger>(relaxed = true)

private val storage = InMemoryStorageTest()
private val dispatcher = InMemoryDispatcherTest(storage)
private val client = dispatcher.client

private lateinit var status: AvroTaskStatus

class WorkflowIntegrationTests : StringSpec({
    val taskTest = TaskTestImpl()
    val workflowTask = WorkflowTaskImpl()
    val workflowA = WorkflowAImpl()
    Worker.register<TaskTest>(taskTest)
    Worker.register<WorkflowTask>(workflowTask)
    Worker.register<WorkflowA>(workflowA)

    var workflowInstance: WorkflowInstance

    beforeTest {
        storage.reset()
        dispatcher.reset()
    }

    "empty Workflow" {
        // run system
        coroutineScope {
            dispatcher.scope = this
            workflowInstance = client.dispatchWorkflow<WorkflowA> { empty() }
        }
        // check that the w is terminated
        storage.isTerminated(workflowInstance) shouldBe true
        // checks number of task processing
        dispatcher.workflowOutput shouldBe "void"
    }

    "Simple Sequential Workflow" {
        // run system
        coroutineScope {
            dispatcher.scope = this
            workflowInstance = client.dispatchWorkflow<WorkflowA> { seq1() }
        }
        // check that the w is terminated
        storage.isTerminated(workflowInstance) shouldBe true
        // checks number of task processing
        dispatcher.workflowOutput shouldBe "123"
    }

    "Sequential Workflow with an async task" {
        // run system
        coroutineScope {
            dispatcher.scope = this
            workflowInstance = client.dispatchWorkflow<WorkflowA> { seq2() }
        }
        // check that the w is terminated
        storage.isTerminated(workflowInstance) shouldBe true
        // checks number of task processing
        dispatcher.workflowOutput shouldBe "23ba"
    }

    "Sequential Workflow with an async branch" {
        // run system
        coroutineScope {
            dispatcher.scope = this
            workflowInstance = client.dispatchWorkflow<WorkflowA> { seq3() }
        }
        // check that the w is terminated
        storage.isTerminated(workflowInstance) shouldBe true
        // checks number of task processing
        dispatcher.workflowOutput shouldBe "23ba"
    }

    "Sequential Workflow with an async branch with 2 tasks" {
        // run system
        coroutineScope {
            dispatcher.scope = this
            workflowInstance = client.dispatchWorkflow<WorkflowA> { seq4() }
        }
        // check that the w is terminated
        storage.isTerminated(workflowInstance) shouldBe true
        // checks number of task processing
        dispatcher.workflowOutput shouldBe "23bac"
    }

    "Or step with 3 async tasks" {
        // run system
        coroutineScope {
            dispatcher.scope = this
            workflowInstance = client.dispatchWorkflow<WorkflowA> { or1() }
        }
        // check that the w is terminated
        storage.isTerminated(workflowInstance) shouldBe true
        // checks number of task processing
        dispatcher.workflowOutput shouldBeIn listOf("ba", "dc", "fe")
    }

    "Combined And/Or step with 3 async tasks" {
        // run system
        coroutineScope {
            dispatcher.scope = this
            workflowInstance = client.dispatchWorkflow<WorkflowA> { or2() }
        }
        // check that the w is terminated
        storage.isTerminated(workflowInstance) shouldBe true
        // checks number of task processing
        dispatcher.workflowOutput shouldBeIn listOf(listOf("ba", "dc"), "fe")
    }

    "And step with 3 async tasks" {
        // run system
        coroutineScope {
            dispatcher.scope = this
            workflowInstance = client.dispatchWorkflow<WorkflowA> { and1() }
        }
        // check that the w is terminated
        storage.isTerminated(workflowInstance) shouldBe true
        // checks number of task processing
        dispatcher.workflowOutput shouldBe listOf("ba", "dc", "fe")
    }
})
