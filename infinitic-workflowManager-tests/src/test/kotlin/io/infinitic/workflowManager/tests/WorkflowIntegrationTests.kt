package io.infinitic.workflowManager.tests

import io.infinitic.taskManager.client.ClientDispatcher
import io.infinitic.taskManager.common.avro.AvroConverter as TaskAvroConverter
import io.infinitic.taskManager.data.AvroTaskStatus
import io.infinitic.taskManager.engine.dispatcher.EngineDispatcher as TaskEngineDispatcher
import io.infinitic.taskManager.engine.engines.MonitoringGlobal
import io.infinitic.taskManager.engine.engines.MonitoringPerName
import io.infinitic.taskManager.engine.engines.TaskEngine
import io.infinitic.taskManager.tests.inMemory.InMemoryDispatcher
import io.infinitic.taskManager.tests.inMemory.InMemoryStorage
import io.infinitic.taskManager.worker.Worker
import io.infinitic.taskManager.worker.WorkerDispatcher
import io.infinitic.workflowManager.common.avro.AvroConverter as WorkflowAvroConverter
import io.infinitic.workflowManager.common.data.workflows.WorkflowInstance
import io.infinitic.workflowManager.engine.dispatcher.Dispatcher as WorkflowEngineDispatcher
import io.infinitic.workflowManager.engine.engines.WorkflowEngine
import io.infinitic.workflowManager.worker.WorkflowTaskImpl
import io.kotest.core.spec.style.StringSpec
import io.mockk.mockk
import org.slf4j.Logger

private val mockLogger = mockk<Logger>(relaxed = true)

private val dispatcher = InMemoryDispatcher()
private val storage = InMemoryStorage()

private val testAvroDispatcher = InMemoryDispatcher()
private val testTaskDispatcher = TaskEngineDispatcher(testAvroDispatcher)
private val testWorkflowDispatcher = WorkflowEngineDispatcher(testAvroDispatcher)
private val testStorage = InMemoryStorage()

private val client = io.infinitic.taskManager.client.Client(ClientDispatcher(testAvroDispatcher))
private val worker = Worker(WorkerDispatcher(testAvroDispatcher))
private val taskEngine = TaskEngine(testStorage, testTaskDispatcher)
private val monitoringPerName = MonitoringPerName(testStorage, testTaskDispatcher)
private val monitoringGlobal = MonitoringGlobal(testStorage)

private val workflowEngine = WorkflowEngine(testStorage, testWorkflowDispatcher)

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
        dispatcher.apply {
            workflowEngineHandle = {
                workflowEngine.handle(WorkflowAvroConverter.fromWorkflowEngine(it))
            }
            taskEngineHandle =
                {
                    taskEngine.handle(TaskAvroConverter.fromTaskEngine(it))
                }
            monitoringPerNameHandle =
                { avro ->
                    monitoringPerName.handle(TaskAvroConverter.fromMonitoringPerName(avro))
                    // update test status
                    avro.taskStatusUpdated?.let { status = it.newStatus }
                }
            monitoringGlobalHandle =
                {
                    monitoringGlobal.handle(TaskAvroConverter.fromMonitoringGlobal(it))
                }
            workerHandle =
                {
                    worker.handle(it)
                }
        }
    }
}
