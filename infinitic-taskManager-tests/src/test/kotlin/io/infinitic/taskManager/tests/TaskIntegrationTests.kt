package io.infinitic.taskManager.tests

import io.infinitic.taskManager.client.Client
import io.infinitic.taskManager.common.avro.AvroConverter
import io.infinitic.taskManager.common.data.TaskInstance
import io.infinitic.taskManager.data.AvroTaskStatus
import io.infinitic.taskManager.engine.dispatcher.Dispatcher
import io.infinitic.taskManager.engine.engines.MonitoringGlobal
import io.infinitic.taskManager.engine.engines.MonitoringPerName
import io.infinitic.taskManager.engine.engines.TaskEngine
import io.infinitic.taskManager.tests.inMemory.InMemoryDispatcher
import io.infinitic.taskManager.tests.inMemory.InMemoryStorage
import io.infinitic.taskManager.worker.Worker
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.mockk.mockk
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import org.slf4j.Logger

private val mockLogger = mockk<Logger>(relaxed = true)
private val client = Client()
private val worker = Worker()
private val testAvroDispatcher = InMemoryDispatcher()
private val testDispatcher = Dispatcher(testAvroDispatcher)
private val testStorage = InMemoryStorage()
private val taskEngine = TaskEngine(testStorage, testDispatcher)
private val monitoringPerName = MonitoringPerName(testStorage, testDispatcher)
private val monitoringGlobal = MonitoringGlobal(testStorage)

private lateinit var status: AvroTaskStatus

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
            task = client.dispatchTask<TaskTest> { log() }
        }
        // check that task is terminated
        testStorage.isTerminated(task) shouldBe true
        // check that task is completed
        status shouldBe AvroTaskStatus.TERMINATED_COMPLETED
        // checks number of task processing
        TaskTestImpl.log shouldBe "1"
    }

    "Task succeeds at 4th try" {
        // task will succeed only at the 4th try
        taskTest.behavior = { _, retry -> if (retry < 3) Status.FAILED_WITH_RETRY else Status.SUCCESS }
        // run system
        coroutineScope {
            testAvroDispatcher.scope = this
            task = client.dispatchTask<TaskTest> { log() }
        }
        // check that task is terminated
        testStorage.isTerminated(task) shouldBe true
        // check that task is completed
        status shouldBe AvroTaskStatus.TERMINATED_COMPLETED
        // checks number of task processing
        TaskTestImpl.log shouldBe "0001"
    }

    "Task fails at first try" {
        // task will succeed only at the 4th try
        taskTest.behavior = { _, _ -> Status.FAILED_WITHOUT_RETRY }
        // run system
        coroutineScope {
            testAvroDispatcher.scope = this
            task = client.dispatchTask<TaskTest> { log() }
        }
        // check that task is not terminated
        testStorage.isTerminated(task) shouldBe false
        // check that task is failed
        status shouldBe AvroTaskStatus.RUNNING_ERROR
        // checks number of task processing
        TaskTestImpl.log shouldBe "0"
    }

    "Task fails after 4 tries " {
        // task will succeed only at the 4th try
        taskTest.behavior = { _, retry -> if (retry < 3) Status.FAILED_WITH_RETRY else Status.FAILED_WITHOUT_RETRY }
        // run system
        coroutineScope {
            testAvroDispatcher.scope = this
            task = client.dispatchTask<TaskTest> { log() }
        }
        // check that task is not terminated
        testStorage.isTerminated(task) shouldBe false
        // check that task is failed
        status shouldBe AvroTaskStatus.RUNNING_ERROR
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
            task = client.dispatchTask<TaskTest> { log() }
            delay(100)
            client.retryTask(id = "${task.taskId}")
        }
        // check that task is terminated
        testStorage.isTerminated(task)
        // check that task is completed
        status shouldBe AvroTaskStatus.TERMINATED_COMPLETED
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
            task = client.dispatchTask<TaskTest> { log() }
            delay(100)
            client.cancelTask(id = "${task.taskId}")
        }
        // check that task is terminated
        testStorage.isTerminated(task)
        // check that task is completed
        status shouldBe AvroTaskStatus.TERMINATED_CANCELED
    }
}) {
    init {
        client.setTaskDispatcher(testAvroDispatcher)

        worker.setAvroDispatcher(testAvroDispatcher)

        testAvroDispatcher.apply {
            taskEngineHandle = {
                taskEngine.handle(AvroConverter.fromTaskEngine(it))
            }
            monitoringPerNameHandle = { avro ->
                monitoringPerName.handle(AvroConverter.fromMonitoringPerName(avro))
                // update test status
                avro.taskStatusUpdated?.let { status = it.newStatus }
            }
            monitoringGlobalHandle = { monitoringGlobal.handle(AvroConverter.fromMonitoringGlobal(it)) }
            workerHandle = { worker.handle(it) }
        }
    }
}
