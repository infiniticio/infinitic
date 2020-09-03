package io.infinitic.taskManager.tests

import io.infinitic.taskManager.client.Client
import io.infinitic.taskManager.engine.avroClasses.AvroTaskEngine
import io.infinitic.taskManager.engine.avroClasses.AvroMonitoringGlobal
import io.infinitic.taskManager.engine.avroClasses.AvroMonitoringPerName
import io.infinitic.taskManager.common.data.TaskInstance
import io.infinitic.taskManager.data.AvroTaskStatus
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
private val taskEngine = AvroTaskEngine()
private val monitoringPerName = AvroMonitoringPerName()
private val monitoringGlobal = AvroMonitoringGlobal()
private val worker = Worker()
private val dispatcher = InMemoryDispatcher()
private val storage = InMemoryStorage()

private lateinit var status: AvroTaskStatus

class TaskIntegrationTests : StringSpec({
    val taskTest = TaskTestImpl()
    Worker.register<TaskTest>(taskTest)
    var taskInstance: TaskInstance

    beforeTest {
        storage.reset()
        TaskTestImpl.log = ""
    }

    "Task succeeds at first try" {
        // task will succeed
        taskTest.behavior = { _, _ -> Status.SUCCESS }
        // run system
        coroutineScope {
            dispatcher.scope = this
            taskInstance = client.dispatchTask<TaskTest> { log() }
        }
        // check that task is terminated
        storage.isTerminated(taskInstance) shouldBe true
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
            dispatcher.scope = this
            taskInstance = client.dispatchTask<TaskTest> { log() }
        }
        // check that task is terminated
        storage.isTerminated(taskInstance) shouldBe true
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
            dispatcher.scope = this
            taskInstance = client.dispatchTask<TaskTest> { log() }
        }
        // check that task is not terminated
        storage.isTerminated(taskInstance) shouldBe false
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
            dispatcher.scope = this
            taskInstance = client.dispatchTask<TaskTest> { log() }
        }
        // check that task is not terminated
        storage.isTerminated(taskInstance) shouldBe false
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
            dispatcher.scope = this
            taskInstance = client.dispatchTask<TaskTest> { log() }
            delay(100)
            client.retryTask(id = taskInstance.taskId.id)
        }
        // check that task is terminated
        storage.isTerminated(taskInstance)
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
            dispatcher.scope = this
            taskInstance = client.dispatchTask<TaskTest> { log() }
            delay(100)
            client.cancelTask(id = taskInstance.taskId.id)
        }
        // check that task is terminated
        storage.isTerminated(taskInstance)
        // check that task is completed
        status shouldBe AvroTaskStatus.TERMINATED_CANCELED
    }
}) {
    init {
        client.setTaskDispatcher(dispatcher)

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
