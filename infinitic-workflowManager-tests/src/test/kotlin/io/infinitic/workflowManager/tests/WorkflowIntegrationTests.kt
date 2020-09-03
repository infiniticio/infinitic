package io.infinitic.workflowManager.tests

import io.infinitic.taskManager.engine.avroClasses.AvroTaskEngine
import io.infinitic.taskManager.engine.avroClasses.AvroMonitoringGlobal
import io.infinitic.taskManager.engine.avroClasses.AvroMonitoringPerName
import io.infinitic.taskManager.data.AvroTaskStatus
import io.infinitic.taskManager.tests.inMemory.InMemoryDispatcher
import io.infinitic.taskManager.tests.inMemory.InMemoryStorage
import io.infinitic.taskManager.worker.Worker
import io.infinitic.workflowManager.client.Client
import io.infinitic.workflowManager.common.data.workflows.WorkflowInstance
import io.infinitic.workflowManager.engine.avroEngines.AvroWorkflowEngine
import io.infinitic.workflowManager.worker.WorkflowTaskImpl
import io.kotest.core.spec.style.StringSpec
import io.mockk.mockk
import org.slf4j.Logger

private val mockLogger = mockk<Logger>(relaxed = true)
private val client = Client()
private val workflowEngine = AvroWorkflowEngine()
private val taskEngine = AvroTaskEngine()
private val monitoringPerName = AvroMonitoringPerName()
private val monitoringGlobal = AvroMonitoringGlobal()
private val worker = Worker()
private val dispatcher = InMemoryDispatcher()
private val storage = InMemoryStorage()

private lateinit var status: AvroTaskStatus

class TaskIntegrationTests : StringSpec({
    val taskTest = TaskTestImpl()
    val workflowTask = WorkflowTaskImpl()
    Worker.register<TaskTest>(taskTest)
    Worker.register<WorkflowTaskImpl>(workflowTask)
    var workflowInstance: WorkflowInstance

    beforeTest {
        storage.reset()
        TaskTestImpl.log = ""
    }

//    "Task succeeds at first try" {
//        // run system
//        coroutineScope {
//            dispatcher.scope = this
//            workflowInstance = client.dispatchWorkflow<WorkflowAImpl> { test1() }
//        }
//        // check that task is terminated
//        storage.isTerminated(workflowInstance) shouldBe true
//        // checks number of task processing
//        TaskTestImpl.log shouldBe "11"
//    }
}) {
    init {
        client.setTaskDispatcher(dispatcher)
        client.setWorkflowDispatcher(dispatcher)

        workflowEngine.apply {
            avroStorage = storage
            avroDispatcher = dispatcher
            logger = mockLogger
        }

        taskEngine.apply {
            avroStorage = storage
            avroDispatcher = dispatcher
            logger = mockLogger
        }

        monitoringPerName.apply {
            avroStorage = storage
            avroDispatcher = dispatcher
            logger = mockLogger
        }

        monitoringGlobal.apply {
            avroStorage = storage
            logger = mockLogger
        }

        worker.setAvroDispatcher(dispatcher)

        dispatcher.apply {
            workflowEngineHandle = { workflowEngine.handle(it) }
            taskEngineHandle = { taskEngine.handle(it) }
            monitoringPerNameHandle = { avro ->
                monitoringPerName.handle(avro)
                // update test status
                avro.taskStatusUpdated?.let { status = it.newStatus }
            }
            monitoringGlobalHandle = { monitoringGlobal.handle(it) }
            workerHandle = { worker.handle(it) }
        }
    }
}
