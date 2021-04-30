/**
 * "Commons Clause" License Condition v1.0
 *
 * The Software is provided to you by the Licensor under the License, as defined
 * below, subject to the following condition.
 *
 * Without limiting other conditions in the License, the grant of rights under the
 * License will not include, and the License does not grant to you, the right to
 * Sell the Software.
 *
 * For purposes of the foregoing, “Sell” means practicing any or all of the rights
 * granted to you under the License to provide to third parties, for a fee or
 * other consideration (including without limitation fees for hosting or
 * consulting/ support services related to the Software), a product or service
 * whose value derives, entirely or substantially, from the functionality of the
 * Software. Any license notice or attribution required by the License must also
 * include this Commons Clause License Condition notice.
 *
 * Software: Infinitic
 *
 * License: MIT License (https://opensource.org/licenses/MIT)
 *
 * Licensor: infinitic.io
 */

@file:Suppress("MoveLambdaOutsideParentheses")

package io.infinitic.inMemory

import io.infinitic.clients.cancelTask
import io.infinitic.clients.newTask
import io.infinitic.clients.retryTask
import io.infinitic.common.tasks.data.TaskId
import io.infinitic.common.tasks.data.TaskName
import io.infinitic.common.tasks.data.TaskTag
import io.infinitic.exceptions.clients.CanceledDeferredException
import io.infinitic.exceptions.clients.FailedDeferredException
import io.infinitic.inMemory.tasks.Status
import io.infinitic.inMemory.tasks.TaskException
import io.infinitic.inMemory.tasks.TaskTest
import io.infinitic.inMemory.tasks.TaskTestImpl
import io.infinitic.tasks.executor.register.TaskExecutorRegisterImpl
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.config.configuration
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

internal class TaskTests : StringSpec({

    // each test should not be longer than 5s (for github)
    configuration.timeout = 5000

    lateinit var behavior: (index: Int, retry: Int) -> Status

    val taskTestImpl = TaskTestImpl()

    val taskExecutorRegister = TaskExecutorRegisterImpl().apply {
        registerTask(TaskTest::class.java.name) {
            taskTestImpl.apply {
                this.behavior = behavior
                this.log = ""
            }
        }
    }

    val client = InfiniticClient(taskExecutorRegister, "client: inMemory")
    val taskTest = client.newTask<TaskTest>()
    val taskTestWithTags = client.newTask<TaskTest>(tags = setOf("foo", "bar"))

    afterSpec {
        client.close()
    }

    "Asynchronous execution succeeds at first try" {
        behavior = { _, _ -> Status.SUCCESS }

        val result = taskTest.log()

        result shouldBe "1"
    }

    "Synchronous execution succeeds at first try" {
        behavior = { _, _ -> Status.SUCCESS }

        val result = taskTest.log()

        result shouldBe "1"
    }

    "Synchronous Task succeeds at 4th try" {
        behavior = { _, retry ->
            when {
                (retry < 3) -> Status.FAILED_WITH_RETRY
                else -> Status.SUCCESS
            }
        }

        val result = taskTest.log()

        result shouldBe "0001"
    }

    "Asynchronous Task succeeds at 4th try" {
        behavior = { _, retry ->
            when {
                (retry < 3) -> Status.FAILED_WITH_RETRY
                else -> Status.SUCCESS
            }
        }

        val deferred = client.async(taskTest) { log() }
        val result = deferred.await()

        result shouldBe "0001"
    }

    "Task fails at first try" {
        behavior = { _, _ -> Status.FAILED_WITHOUT_RETRY }

        val e = shouldThrow<FailedDeferredException> { taskTest.log() }

        e.causeError?.errorName shouldBe TaskException::class.java.name
        taskTestImpl.log shouldBe "0"
    }

    "Task fails after 4 tries " {
        // task will failed and stop retries after 3rd
        behavior = { _, retry ->
            when {
                retry < 3 -> Status.FAILED_WITH_RETRY
                else -> Status.FAILED_WITHOUT_RETRY
            }
        }

        val e = shouldThrow<FailedDeferredException> { taskTest.log() }

        e.causeError?.errorName shouldBe TaskException::class.java.name
        taskTestImpl.log shouldBe "0000"
    }

    "Task succeeds after manual retry" {
        // task will succeed only after manual retry
        behavior = { index, _ ->
            when (index) {
                0 -> Status.FAILED_WITHOUT_RETRY
                else -> Status.SUCCESS
            }
        }

        shouldThrow<FailedDeferredException> { taskTest.log() }

        client.retry(taskTest)

        client.await(taskTest) shouldBe "01"
    }

    "Task succeeds after automatic and manual retry" {
        // task will succeed only after manual retry
        behavior = { index, retry ->
            when (index) {
                0 -> if (retry < 3) Status.FAILED_WITH_RETRY else Status.FAILED_WITHOUT_RETRY
                else -> if (retry < 3) Status.FAILED_WITH_RETRY else Status.SUCCESS
            }
        }

        shouldThrow<FailedDeferredException> { taskTest.log() }

        client.retry(taskTest)

        client.await(taskTest) shouldBe "00000001"
    }

    "Task succeeds after manual retry using tags" {
        // task will succeed only after manual retry
        behavior = { index, retry ->
            when (index) {
                0 -> if (retry < 3) Status.FAILED_WITH_RETRY else Status.FAILED_WITHOUT_RETRY
                else -> Status.SUCCESS
            }
        }
        val deferred = client.async(taskTestWithTags) { log() }

        shouldThrow<FailedDeferredException> { deferred.await() }

        client.retryTask<TaskTest>("foo")

        deferred.await() shouldBe "00001"
    }

    "Task canceled during automatic retry" {
        behavior = { _, _ -> Status.FAILED_WITH_RETRY }

        val deferred = client.async(taskTest) { log() }

        launch {
            delay(50)
            client.cancel(taskTest)
        }

        shouldThrow<CanceledDeferredException> { deferred.await() }
    }

    "Multiple Tasks canceled using A tag" {
        behavior = { _, _ -> Status.FAILED_WITH_RETRY }

        val deferred1 = client.async(taskTestWithTags) { log() }
        val deferred2 = client.async(taskTestWithTags) { log() }

        coroutineScope {
            launch {
                shouldThrow<CanceledDeferredException> { deferred1.await() }
            }
            launch {
                shouldThrow<CanceledDeferredException> { deferred2.await() }
            }
            launch {
                delay(50)
                client.cancelTask<TaskTest>("foo")
            }
        }
    }

    "Tag should be added then deleted after completion" {
        behavior = { _, _ -> Status.SUCCESS }

        val deferred = client.async(taskTestWithTags) { await(100) }
        val taskId = TaskId(deferred.id)

        delay(50)
        client.taskTagStorage.getTaskIds(TaskTag("foo"), TaskName(TaskTest::class.java.name)).contains(taskId) shouldBe true
        client.taskTagStorage.getTaskIds(TaskTag("bar"), TaskName(TaskTest::class.java.name)).contains(taskId) shouldBe true

        deferred.await()

        delay(50)
        // checks taskId has been removed from tag storage
        client.taskTagStorage.getTaskIds(TaskTag("foo"), TaskName(TaskTest::class.java.name)).contains(taskId) shouldBe false
        client.taskTagStorage.getTaskIds(TaskTag("bar"), TaskName(TaskTest::class.java.name)).contains(taskId) shouldBe false
    }

    "Tag should be added then deleted after cancellation" {
        behavior = { _, _ -> Status.FAILED_WITH_RETRY }

        val deferred = client.async(taskTestWithTags) { log() }
        val taskId = TaskId(deferred.id)

        delay(50)
        client.taskTagStorage.getTaskIds(TaskTag("foo"), TaskName(TaskTest::class.java.name)).contains(taskId) shouldBe true
        client.taskTagStorage.getTaskIds(TaskTag("bar"), TaskName(TaskTest::class.java.name)).contains(taskId) shouldBe true

        launch {
            client.cancel(taskTestWithTags)
        }

        shouldThrow<CanceledDeferredException> { deferred.await() }

        delay(50)
        client.taskTagStorage.getTaskIds(TaskTag("foo"), TaskName(TaskTest::class.java.name)).contains(taskId) shouldBe false
        client.taskTagStorage.getTaskIds(TaskTag("bar"), TaskName(TaskTest::class.java.name)).contains(taskId) shouldBe false
    }
})
