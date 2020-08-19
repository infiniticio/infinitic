package com.zenaton.taskManager.tests

import com.zenaton.taskManager.client.Client
import com.zenaton.taskManager.engine.avroClasses.AvroTaskEngine
import com.zenaton.taskManager.engine.avroClasses.AvroMonitoringGlobal
import com.zenaton.taskManager.engine.avroClasses.AvroMonitoringPerName
import com.zenaton.taskManager.common.data.Task
import com.zenaton.taskManager.data.AvroTaskStatus
import com.zenaton.taskManager.tests.inMemory.InMemoryDispatcher
import com.zenaton.taskManager.tests.inMemory.InMemoryStorage
import com.zenaton.taskManager.worker.Worker
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.mockk.mockk
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import org.slf4j.Logger

private val mockLogger = mockk<Logger>(relaxed = true)
private val client = Client()
private val jobEngine = AvroTaskEngine()
private val monitoringPerName = AvroMonitoringPerName()
private val monitoringGlobal = AvroMonitoringGlobal()
private val worker = Worker()
private val dispatcher = InMemoryDispatcher()
private val storage = InMemoryStorage()

private lateinit var status: AvroTaskStatus

class AvroTaskEngineTests : StringSpec({
    val taskTest = TaskTestImpl()
    Worker.register<TaskTest>(taskTest)
    var task: Task

    beforeTest {
        storage.reset()
        TaskTestImpl.log = ""
    }

    "Task succeeds at first try" {
        // job will succeed
        taskTest.behavior = { _, _ -> Status.SUCCESS }
        // run system
        coroutineScope {
            dispatcher.scope = this
            task = client.dispatchTask<TaskTest> { log() }
        }
        // check that job is terminated
        storage.isTerminated(task) shouldBe true
        // check that job is completed
        status shouldBe AvroTaskStatus.TERMINATED_COMPLETED
        // checks number of job processing
        TaskTestImpl.log shouldBe "1"
    }

    "Task succeeds at 4th try" {
        // job will succeed only at the 4th try
        taskTest.behavior = { _, retry -> if (retry < 3) Status.FAILED_WITH_RETRY else Status.SUCCESS }
        // run system
        coroutineScope {
            dispatcher.scope = this
            task = client.dispatchTask<TaskTest> { log() }
        }
        // check that job is terminated
        storage.isTerminated(task) shouldBe true
        // check that job is completed
        status shouldBe AvroTaskStatus.TERMINATED_COMPLETED
        // checks number of job processing
        TaskTestImpl.log shouldBe "0001"
    }

    "Task fails at first try" {
        // job will succeed only at the 4th try
        taskTest.behavior = { _, _ -> Status.FAILED_WITHOUT_RETRY }
        // run system
        coroutineScope {
            dispatcher.scope = this
            task = client.dispatchTask<TaskTest> { log() }
        }
        // check that job is not terminated
        storage.isTerminated(task) shouldBe false
        // check that job is failed
        status shouldBe AvroTaskStatus.RUNNING_ERROR
        // checks number of job processing
        TaskTestImpl.log shouldBe "0"
    }

    "Task fails after 4 tries " {
        // job will succeed only at the 4th try
        taskTest.behavior = { _, retry -> if (retry < 3) Status.FAILED_WITH_RETRY else Status.FAILED_WITHOUT_RETRY }
        // run system
        coroutineScope {
            dispatcher.scope = this
            task = client.dispatchTask<TaskTest> { log() }
        }
        // check that job is not terminated
        storage.isTerminated(task) shouldBe false
        // check that job is failed
        status shouldBe AvroTaskStatus.RUNNING_ERROR
        // checks number of job processing
        TaskTestImpl.log shouldBe "0000"
    }

    "Task succeeds after manual retry" {
        // job will succeed only at the 4th try
        taskTest.behavior = { index, retry ->
            if (index == 0)
                if (retry < 3) Status.FAILED_WITH_RETRY else Status.FAILED_WITHOUT_RETRY
            else if (retry < 2) Status.FAILED_WITH_RETRY else Status.SUCCESS
        }
        // run system
        coroutineScope {
            dispatcher.scope = this
            task = client.dispatchTask<TaskTest> { log() }
            delay(100)
            client.retryTask(id = task.taskId.id)
        }
        // check that job is terminated
        storage.isTerminated(task)
        // check that job is completed
        status shouldBe AvroTaskStatus.TERMINATED_COMPLETED
        // checks number of job processing
        TaskTestImpl.log shouldBe "0000001"
    }

    "Task canceled during automatic retry" {
        // job will succeed only at the 4th try
        taskTest.behavior = { _, _ -> Status.FAILED_WITH_RETRY }
        // run system
        // run system
        coroutineScope {
            dispatcher.scope = this
            task = client.dispatchTask<TaskTest> { log() }
            delay(100)
            client.cancelTask(id = task.taskId.id)
        }
        // check that job is terminated
        storage.isTerminated(task)
        // check that job is completed
        status shouldBe AvroTaskStatus.TERMINATED_CANCELED
    }
}) {
    init {
        client.setAvroDispatcher(dispatcher)

        jobEngine.apply {
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
            jobEngineHandle = { jobEngine.handle(it) }
            monitoringPerNameHandle = { avro ->
                monitoringPerName.handle(avro)
                avro.taskStatusUpdated?.let { status = it.newStatus }
            }
            monitoringGlobalHandle = { monitoringGlobal.handle(it) }
            workerHandle = { worker.handle(it) }
        }
    }
}
