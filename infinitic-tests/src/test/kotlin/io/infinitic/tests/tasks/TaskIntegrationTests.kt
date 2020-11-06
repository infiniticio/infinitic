// "Commons Clause" License Condition v1.0
//
// The Software is provided to you by the Licensor under the License, as defined
// below, subject to the following condition.
//
// Without limiting other conditions in the License, the grant of rights under the
// License will not include, and the License does not grant to you, the right to
// Sell the Software.
//
// For purposes of the foregoing, “Sell” means practicing any or all of the rights
// granted to you under the License to provide to third parties, for a fee or
// other consideration (including without limitation fees for hosting or
// consulting/ support services related to the Software), a product or service
// whose value derives, entirely or substantially, from the functionality of the
// Software. Any license notice or attribution required by the License must also
// include this Commons Clause License Condition notice.
//
// Software: Infinitic
//
// License: MIT License (https://opensource.org/licenses/MIT)
//
// Licensor: infinitic.io

package io.infinitic.tests.tasks

import io.infinitic.common.tasks.data.TaskInstance
import io.infinitic.common.tasks.data.TaskStatus
import io.infinitic.storage.inmemory.inMemory
import io.infinitic.tests.tasks.samples.Status
import io.infinitic.tests.tasks.samples.TaskTest
import io.infinitic.tests.tasks.samples.TaskTestImpl
import io.infinitic.tests.tasks.inMemory.InMemoryDispatcherTest
import io.infinitic.tests.tasks.inMemory.InMemoryStorageTest
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay

private val internalStorage = inMemory()
private val storage = InMemoryStorageTest(internalStorage)
private val dispatcher = InMemoryDispatcherTest(storage)
private val client = dispatcher.client
private val worker = dispatcher.worker

class TaskIntegrationTests : StringSpec({
    val taskTest = TaskTestImpl()
    worker.register(TaskTest::class.java.name) { taskTest }
    var task: TaskInstance

    beforeTest {
        storage.reset()
        dispatcher.reset()
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
        taskTest.log shouldBe "1"
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
        taskTest.log shouldBe "0001"
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
        taskTest.log shouldBe "0"
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
        taskTest.log shouldBe "0000"
    }

    "Task succeeds after manual retry" {
        // task will succeed only at the 4th try
        taskTest.behavior = { index, retry ->
            if (index == 0) {
                if (retry < 3) Status.FAILED_WITH_RETRY else Status.FAILED_WITHOUT_RETRY
            } else {
                if (retry < 2) Status.FAILED_WITH_RETRY else Status.SUCCESS
            }
        }
        // run system
        coroutineScope {
            dispatcher.scope = this
            task = client.dispatch(TaskTest::class.java) { log() }
            while (dispatcher.taskStatus != TaskStatus.RUNNING_ERROR ) {
                delay(50)
            }
            client.retryTask(id = "${task.taskId}")
        }
        // check that task is terminated
        storage.isTerminated(task)
        // check that task is completed
        dispatcher.taskStatus shouldBe TaskStatus.TERMINATED_COMPLETED
        // checks number of task processing
        taskTest.log shouldBe "0000001"
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
