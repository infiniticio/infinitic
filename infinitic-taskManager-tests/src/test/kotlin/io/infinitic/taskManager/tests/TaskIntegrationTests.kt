package io.infinitic.taskManager.tests

import io.infinitic.taskManager.client.Client
import io.infinitic.taskManager.client.ClientDispatcher
import io.infinitic.taskManager.common.avro.AvroConverter
import io.infinitic.taskManager.common.data.TaskInstance
import io.infinitic.taskManager.common.data.TaskStatus
import io.infinitic.taskManager.common.messages.TaskStatusUpdated
import io.infinitic.taskManager.engine.engines.MonitoringGlobal
import io.infinitic.taskManager.engine.engines.MonitoringPerName
import io.infinitic.taskManager.engine.engines.TaskEngine
import io.infinitic.taskManager.tests.inMemory.InMemoryDispatcher
import io.infinitic.taskManager.tests.inMemory.InMemoryStorage
import io.infinitic.taskManager.worker.WorkerDispatcher
import io.infinitic.taskManager.worker.Worker
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay

private val testAvroDispatcher = InMemoryDispatcher()
private val testStorage = InMemoryStorage()
private val testEngineDispatcher = io.infinitic.messaging.api.dispatcher.InMemoryDispatcher()

private val client = Client(ClientDispatcher(testAvroDispatcher))
private val worker = Worker(WorkerDispatcher(testAvroDispatcher))
private val taskEngine = TaskEngine(testStorage, testEngineDispatcher)
private val monitoringPerName = MonitoringPerName(testStorage, testEngineDispatcher)
private val monitoringGlobal = MonitoringGlobal(testStorage)

private lateinit var status: TaskStatus

class TaskIntegrationTests : StringSpec({
    val taskTest = TaskTestImpl()
    Worker.register<TaskTest>(taskTest)
    var task: TaskInstance

    beforeTest {
        testStorage.reset()
        TaskTestImpl.log = ""
    }

    "Task succeeds at first try" {
        // task will succeed
        taskTest.behavior = { _, _ -> Status.SUCCESS }
        // run system
        coroutineScope {
            testAvroDispatcher.scope = this
            testEngineDispatcher.scope = this
            task = client.dispatchTask<TaskTest> { log() }
        }
        // check that task is terminated
        testStorage.isTerminated(task) shouldBe true
        // check that task is completed
        status shouldBe TaskStatus.TERMINATED_COMPLETED
        // checks number of task processing
        TaskTestImpl.log shouldBe "1"
    }

    "Task succeeds at 4th try" {
        // task will succeed only at the 4th try
        taskTest.behavior = { _, retry -> if (retry < 3) Status.FAILED_WITH_RETRY else Status.SUCCESS }
        // run system
        coroutineScope {
            testAvroDispatcher.scope = this
            testEngineDispatcher.scope = this
            task = client.dispatchTask<TaskTest> { log() }
        }
        // check that task is terminated
        testStorage.isTerminated(task) shouldBe true
        // check that task is completed
        status shouldBe TaskStatus.TERMINATED_COMPLETED
        // checks number of task processing
        TaskTestImpl.log shouldBe "0001"
    }

    "Task fails at first try" {
        // task will succeed only at the 4th try
        taskTest.behavior = { _, _ -> Status.FAILED_WITHOUT_RETRY }
        // run system
        coroutineScope {
            testAvroDispatcher.scope = this
            testEngineDispatcher.scope = this
            task = client.dispatchTask<TaskTest> { log() }
        }
        // check that task is not terminated
        testStorage.isTerminated(task) shouldBe false
        // check that task is failed
        status shouldBe TaskStatus.RUNNING_ERROR
        // checks number of task processing
        TaskTestImpl.log shouldBe "0"
    }

    "Task fails after 4 tries " {
        // task will succeed only at the 4th try
        taskTest.behavior = { _, retry -> if (retry < 3) Status.FAILED_WITH_RETRY else Status.FAILED_WITHOUT_RETRY }
        // run system
        coroutineScope {
            testAvroDispatcher.scope = this
            testEngineDispatcher.scope = this
            task = client.dispatchTask<TaskTest> { log() }
        }
        // check that task is not terminated
        testStorage.isTerminated(task) shouldBe false
        // check that task is failed
        status shouldBe TaskStatus.RUNNING_ERROR
        // checks number of task processing
        TaskTestImpl.log shouldBe "0000"
    }

    "Task succeeds after manual retry" {
        // task will succeed only at the 4th try
        taskTest.behavior = { index, retry ->
            if (index == 0)
                if (retry < 3) Status.FAILED_WITH_RETRY else Status.FAILED_WITHOUT_RETRY
            else if (retry < 2) Status.FAILED_WITH_RETRY else Status.SUCCESS
        }
        // run system
        coroutineScope {
            testAvroDispatcher.scope = this
            testEngineDispatcher.scope = this
            task = client.dispatchTask<TaskTest> { log() }
            delay(100)
            client.retryTask(id = "${task.taskId}")
        }
        // check that task is terminated
        testStorage.isTerminated(task)
        // check that task is completed
        status shouldBe TaskStatus.TERMINATED_COMPLETED
        // checks number of task processing
        TaskTestImpl.log shouldBe "0000001"
    }

    "Task canceled during automatic retry" {
        // task will succeed only at the 4th try
        taskTest.behavior = { _, _ -> Status.FAILED_WITH_RETRY }
        // run system
        // run system
        coroutineScope {
            testAvroDispatcher.scope = this
            testEngineDispatcher.scope = this
            task = client.dispatchTask<TaskTest> { log() }
            delay(100)
            client.cancelTask(id = "${task.taskId}")
        }
        // check that task is terminated
        testStorage.isTerminated(task)
        // check that task is completed
        status shouldBe TaskStatus.TERMINATED_CANCELED
    }
}) {
    init {
        testAvroDispatcher.apply {
            taskEngineHandle =
                {
                    taskEngine.handle(AvroConverter.fromTaskEngine(it))
                }
            monitoringPerNameHandle =
                {
                    monitoringPerName.handle(AvroConverter.fromMonitoringPerName(it))
                }
            monitoringGlobalHandle =
                {
                    monitoringGlobal.handle(AvroConverter.fromMonitoringGlobal(it))
                }
            workerHandle =
                {
                    worker.handle(it)
                }
        }

        testEngineDispatcher.apply {
            taskEngineHandle = {
                taskEngine.handle(it)
            }
            monitoringPerNameHandle = {
                monitoringPerName.handle(it)
                when (it) {
                    is TaskStatusUpdated -> {
                        status = it.newStatus
                    }
                }
            }
            monitoringGlobalHandle = {
                monitoringGlobal.handle(it)
            }
            workerHandle = {
                worker.handle(it)
            }
        }
    }
}
