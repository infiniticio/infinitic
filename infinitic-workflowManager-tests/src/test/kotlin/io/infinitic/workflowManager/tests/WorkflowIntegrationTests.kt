package io.infinitic.workflowManager.tests

import io.infinitic.taskManager.data.AvroTaskStatus
import io.infinitic.taskManager.tests.inMemory.InMemoryDispatcherTest
import io.infinitic.taskManager.tests.inMemory.InMemoryStorageTest
import io.infinitic.taskManager.worker.Worker
import io.infinitic.workflowManager.common.data.methodRuns.MethodOutput
import io.infinitic.workflowManager.common.data.workflows.WorkflowInstance
import io.infinitic.workflowManager.common.data.workflowTasks.WorkflowTask
import io.infinitic.workflowManager.worker.data.WorkflowTaskImpl
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.mockk.mockk
import kotlinx.coroutines.coroutineScope
import org.slf4j.Logger

private val mockLogger = mockk<Logger>(relaxed = true)

private val storage = InMemoryStorageTest()
private val dispatcher = InMemoryDispatcherTest(storage)
private val client = dispatcher.client

private lateinit var status: AvroTaskStatus

class TaskIntegrationTests : StringSpec({
    val taskTest = TaskTestImpl()
    val workflowTask = WorkflowTaskImpl()
    val workflowA = WorkflowAImpl()
    Worker.register<TaskTest>(taskTest)
    Worker.register<WorkflowTask>(workflowTask)
    Worker.register<WorkflowA>(workflowA)

    var workflowInstance: WorkflowInstance

    beforeTest {
        storage.reset()
    }

    "Simple Sequential Workflow" {
        // run system
        coroutineScope {
            dispatcher.scope = this
            workflowInstance = client.dispatchWorkflow<WorkflowA> { test1() }
        }
        // check that the w is terminated
        storage.isTerminated(workflowInstance) shouldBe true
        // checks number of task processing
        dispatcher.workflowOutput shouldBe MethodOutput("123")
    }
})
