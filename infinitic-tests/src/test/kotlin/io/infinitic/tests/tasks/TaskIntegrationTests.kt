package io.infinitic.tests.tasks

import io.infinitic.common.tasks.data.TaskInstance
import io.infinitic.common.tasks.data.TaskStatus
import io.infinitic.tests.tasks.samples.Status
import io.infinitic.tests.tasks.samples.TaskTest
import io.infinitic.tests.tasks.samples.TaskTestImpl
import io.infinitic.tests.tasks.inMemory.InMemoryDispatcherTest
import io.infinitic.tests.tasks.inMemory.InMemoryStorageTest
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay

private val storage = InMemoryStorageTest()
private val dispatcher = InMemoryDispatcherTest(storage)
private val client = dispatcher.client
private val worker = dispatcher.worker

class TaskIntegrationTests : StringSpec({
    val taskTest = TaskTestImpl()
    worker.register(TaskTest::class.java.name, taskTest)
    var task: TaskInstance

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
            task = client.dispatch(TaskTest::class.java) { log() }
        }
        // check that task is terminated
        storage.isTerminated(task) shouldBe true
        // check that task is completed
        dispatcher.taskStatus shouldBe TaskStatus.TERMINATED_COMPLETED
        // checks number of task processing
        TaskTestImpl.log shouldBe "1"
    }

    "Task succeeds at 4th try" {
        // task will succeed only at the 4th try
        taskTest.behavior = { _, retry -> if (retry < 3) Status.FAILED_WITH_RETRY else Status.SUCCESS }
        // run system
        coroutineScope {
            dispatcher.scope = this
            task = client.dispatch(TaskTest::class.java) { log() }
        }
        // check that task is terminated
        storage.isTerminated(task) shouldBe true
        // check that task is completed
        dispatcher.taskStatus shouldBe TaskStatus.TERMINATED_COMPLETED
        // checks number of task processing
        TaskTestImpl.log shouldBe "0001"
    }

    "Task fails at first try" {
        // task will succeed only at the 4th try
        taskTest.behavior = { _, _ -> Status.FAILED_WITHOUT_RETRY }
        // run system
        coroutineScope {
            dispatcher.scope = this
            task = client.dispatch(TaskTest::class.java) { log() }
        }
        // check that task is not terminated
        storage.isTerminated(task) shouldBe false
        // check that task is failed
        dispatcher.taskStatus shouldBe TaskStatus.RUNNING_ERROR
        // checks number of task processing
        TaskTestImpl.log shouldBe "0"
    }

    "Task fails after 4 tries " {
        // task will succeed only at the 4th try
        taskTest.behavior = { _, retry -> if (retry < 3) Status.FAILED_WITH_RETRY else Status.FAILED_WITHOUT_RETRY }
        // run system
        coroutineScope {
            dispatcher.scope = this
            task = client.dispatch(TaskTest::class.java) { log() }
        }
        // check that task is not terminated
        storage.isTerminated(task) shouldBe false
        // check that task is failed
        dispatcher.taskStatus shouldBe TaskStatus.RUNNING_ERROR
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
            task = client.dispatch(TaskTest::class.java) { log() }
            delay(100)
            client.retryTask(id = "${task.taskId}")
        }
        // check that task is terminated
        storage.isTerminated(task)
        // check that task is completed
        dispatcher.taskStatus shouldBe TaskStatus.TERMINATED_COMPLETED
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
            task = client.dispatch(TaskTest::class.java) { log() }
            delay(100)
            client.cancelTask(id = "${task.taskId}")
        }
        // check that task is terminated
        storage.isTerminated(task)
        // check that task is completed
        dispatcher.taskStatus shouldBe TaskStatus.TERMINATED_CANCELED
    }
})
