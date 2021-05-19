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

// @file:Suppress("MoveLambdaOutsideParentheses")

package io.infinitic.tests.pulsar

import io.infinitic.client.cancelTask
import io.infinitic.client.newTask
import io.infinitic.client.retryTask
import io.infinitic.common.tasks.data.TaskMeta
import io.infinitic.exceptions.clients.CanceledDeferredException
import io.infinitic.exceptions.clients.FailedDeferredException
import io.infinitic.pulsar.PulsarInfiniticClient
import io.infinitic.pulsar.PulsarInfiniticWorker
import io.infinitic.tests.tasks.Status
import io.infinitic.tests.tasks.TaskA
import io.infinitic.tests.tasks.TaskException
import io.infinitic.tests.tasks.TaskTest
import io.infinitic.tests.tasks.TaskTestImpl
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.config.configuration
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.delay
import kotlinx.coroutines.future.future
import kotlinx.coroutines.launch
import kotlin.concurrent.thread

internal class TaskTests : StringSpec({

    // each test should not be longer than 10s (for github)
    configuration.timeout = 10000

    val client = PulsarInfiniticClient.fromConfigResource("/pulsar.yml")
    val worker = PulsarInfiniticWorker.fromConfigResource("/pulsar.yml")

    val taskTest = client.newTask<TaskTest>()
    val taskTestWithTags = client.newTask<TaskTest>(tags = setOf("foo", "bar"))

    beforeTest {
        worker.storageFlush()
    }

    beforeSpec {
        thread { worker.start() }
    }

    afterSpec {
        worker.close()
    }

    "Asynchronous execution succeeds at first try" {
        TaskTestImpl.behavior = { _, _ -> Status.SUCCESS }

        val result = client.async(taskTest) { log() }.await()

        result shouldBe "1"
    }

    "Synchronous execution succeeds at first try" {
        TaskTestImpl.behavior = { _, _ -> Status.SUCCESS }

        val result = taskTest.log()

        result shouldBe "1"
    }

    "Synchronous Task succeeds at 4th try" {
        TaskTestImpl.behavior = { _, retry ->
            when {
                (retry < 3) -> Status.FAILED_WITH_RETRY
                else -> Status.SUCCESS
            }
        }

        val result = taskTest.log()

        result shouldBe "0001"
    }

    "Asynchronous Task succeeds at 4th try" {
        TaskTestImpl.behavior = { _, retry ->
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
        TaskTestImpl.behavior = { _, _ -> Status.FAILED_WITHOUT_RETRY }

        val e = shouldThrow<FailedDeferredException> { taskTest.log() }

        e.causeError?.errorName shouldBe TaskException::class.java.name
    }

    "Task fails after 4 tries " {
        // task will failed and stop retries after 3rd
        TaskTestImpl.behavior = { _, retry ->
            when {
                retry < 3 -> Status.FAILED_WITH_RETRY
                else -> Status.FAILED_WITHOUT_RETRY
            }
        }

        val e = shouldThrow<FailedDeferredException> { taskTest.log() }

        e.causeError?.errorName shouldBe TaskException::class.java.name
    }

    "Task succeeds after manual retry" {
        // task will succeed only after manual retry
        TaskTestImpl.behavior = { index, _ ->
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
        TaskTestImpl.behavior = { index, retry ->
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
        TaskTestImpl.behavior = { index, retry ->
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
        TaskTestImpl.behavior = { _, _ -> Status.FAILED_WITH_RETRY }

        val deferred = client.async(taskTest) { log() }

        val job = client.scope.future {
            delay(100)
            client.cancel(taskTest)
        }

        shouldThrow<CanceledDeferredException> { deferred.await() }

        job.join()
    }

    "Multiple Tasks canceled using A tag" {
        TaskTestImpl.behavior = { _, _ -> Status.FAILED_WITH_RETRY }

        val deferred1 = client.async(taskTestWithTags) { log() }
        val deferred2 = client.async(taskTestWithTags) { log() }

        client.scope.future {
            launch {
                shouldThrow<CanceledDeferredException> { deferred1.await() }
            }
            launch {
                shouldThrow<CanceledDeferredException> { deferred2.await() }
            }
            launch {
                delay(100)
                client.cancelTask<TaskTest>("foo")
            }
        }.join()
    }

//    "Tag should be added then deleted after completion" {
//        TaskTestImpl.behavior = { _, _ -> Status.SUCCESS }
//
//        val deferred = client.async(taskTestWithTags) { await(100) }
//        val taskId = TaskId(deferred.id)
//
//        delay(50)
//        client.taskTagStorage.getTaskIds(TaskTag("foo"), TaskName(TaskTest::class.java.name)).contains(taskId) shouldBe true
//        client.taskTagStorage.getTaskIds(TaskTag("bar"), TaskName(TaskTest::class.java.name)).contains(taskId) shouldBe true
//
//        deferred.await()
//
//        delay(50)
//        // checks taskId has been removed from tag storage
//        client.taskTagStorage.getTaskIds(TaskTag("foo"), TaskName(TaskTest::class.java.name)).contains(taskId) shouldBe false
//        client.taskTagStorage.getTaskIds(TaskTag("bar"), TaskName(TaskTest::class.java.name)).contains(taskId) shouldBe false
//    }
//
//    "Tag should be added then deleted after cancellation" {
//        TaskTestImpl.behavior = { _, _ -> Status.FAILED_WITH_RETRY }
//
//        val deferred = client.async(taskTestWithTags) { log() }
//        val taskId = TaskId(deferred.id)
//
//        delay(50)
//        client.taskTagStorage.getTaskIds(TaskTag("foo"), TaskName(TaskTest::class.java.name)).contains(taskId) shouldBe true
//        client.taskTagStorage.getTaskIds(TaskTag("bar"), TaskName(TaskTest::class.java.name)).contains(taskId) shouldBe true
//
//        val job = client.scope.future {
//            delay(100)
//            client.cancel(taskTestWithTags)
//        }
//
//        shouldThrow<CanceledDeferredException> { deferred.await() }
//
//        delay(50)
//        client.taskTagStorage.getTaskIds(TaskTag("foo"), TaskName(TaskTest::class.java.name)).contains(taskId) shouldBe false
//        client.taskTagStorage.getTaskIds(TaskTag("bar"), TaskName(TaskTest::class.java.name)).contains(taskId) shouldBe false
//
//        job.join()
//    }

    "get tags from context" {
        val taskWithTags = client.newTask<TaskA>(tags = setOf("foo", "bar"))

        val result = taskWithTags.tags()

        result shouldBe setOf("foo", "bar")
    }

    "get meta from context" {
        val taskWithMeta = client.newTask<TaskA>(meta = mapOf("foo" to "bar".toByteArray()))

        val result = taskWithMeta.meta()

        result shouldBe TaskMeta(mapOf("foo" to "bar".toByteArray()))
    }
})
