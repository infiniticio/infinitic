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

package io.infinitic.tests

import io.infinitic.common.fixtures.later
import io.infinitic.common.tasks.data.TaskMeta
import io.infinitic.exceptions.clients.CanceledException
import io.infinitic.exceptions.clients.FailedException
import io.infinitic.factory.InfiniticClientFactory
import io.infinitic.factory.InfiniticWorkerFactory
import io.infinitic.tests.tasks.ExpectedException
import io.infinitic.tests.tasks.Status
import io.infinitic.tests.tasks.TaskA
import io.infinitic.tests.tasks.TaskTest
import io.infinitic.tests.tasks.TaskTestImpl
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.config.configuration
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.concurrent.thread

internal class TaskTests : StringSpec({

    // each test should not be longer than 5s
    configuration.timeout = 5000

    val client = autoClose(InfiniticClientFactory.fromConfigResource("/pulsar.yml"))
    val worker = autoClose(InfiniticWorkerFactory.fromConfigResource("/pulsar.yml"))

    val taskTest = client.taskStub(TaskTest::class.java)
    val taskTestWithTags = client.taskStub(TaskTest::class.java, tags = setOf("foo", "bar"))

    beforeTest {
        worker.storageFlush()
    }

    beforeSpec {
        thread { worker.start() }
    }

    "Asynchronous execution succeeds at first try" {
        TaskTestImpl.behavior = { _, _ -> Status.SUCCESS }

        val deferred = client.start(taskTest::await).with(400L).join()

        deferred.await() shouldBe 400L
    }

    "Synchronous execution succeeds at first try" {
        TaskTestImpl.behavior = { _, _ -> Status.SUCCESS }

        taskTest.log() shouldBe "1"
    }

    "Synchronous Task succeeds at 10th try" {
        TaskTestImpl.behavior = { _, retry ->
            when {
                (retry < 10) -> Status.FAILED_WITH_RETRY
                else -> Status.SUCCESS
            }
        }

        taskTest.log() shouldBe "00000000001"
    }

    "Asynchronous Task succeeds at 10th try" {
        TaskTestImpl.behavior = { _, retry ->
            when {
                (retry < 10) -> Status.FAILED_WITH_RETRY
                else -> Status.SUCCESS
            }
        }

        val deferred = client.start(taskTest::log).with().join()

        deferred.await() shouldBe "00000000001"
    }

    "Task fails at first try" {
        TaskTestImpl.behavior = { _, _ -> Status.FAILED_WITHOUT_RETRY }

        val e = shouldThrow<FailedException> { taskTest.log() }

        e.causeError?.errorName shouldBe ExpectedException::class.java.name
    }

    "Task fails after 4 tries " {
        // task will fail and stop retries after 3rd
        TaskTestImpl.behavior = { _, retry ->
            when {
                retry < 3 -> Status.FAILED_WITH_RETRY
                else -> Status.FAILED_WITHOUT_RETRY
            }
        }

        val e = shouldThrow<FailedException> { taskTest.log() }

        e.causeError?.errorName shouldBe ExpectedException::class.java.name
    }

    "Task succeeds after manual retry" {
        // task will succeed only after manual retry
        TaskTestImpl.behavior = { index, _ ->
            when (index) {
                0 -> Status.FAILED_WITHOUT_RETRY
                else -> Status.SUCCESS
            }
        }

        val deferred = client.start(taskTest::log).with().join()

        shouldThrow<FailedException> { deferred.await() }

        deferred.retry().join()

        deferred.await() shouldBe "01"
    }

    "Task succeeds after automatic and manual retry" {
        // task will succeed only after manual retry
        TaskTestImpl.behavior = { index, retry ->
            when (index) {
                0 -> if (retry < 3) Status.FAILED_WITH_RETRY else Status.FAILED_WITHOUT_RETRY
                else -> if (retry < 3) Status.FAILED_WITH_RETRY else Status.SUCCESS
            }
        }

        val deferred = client.start(taskTest::log).with().join()

        shouldThrow<FailedException> { deferred.await() }

        deferred.retry().join()

        deferred.await() shouldBe "00000001"
    }

    "Task succeeds after manual retry using tags" {
        // task will succeed only after manual retry
        TaskTestImpl.behavior = { index, retry ->
            when (index) {
                0 -> if (retry < 3) Status.FAILED_WITH_RETRY else Status.FAILED_WITHOUT_RETRY
                else -> Status.SUCCESS
            }
        }
        val deferred = client.start(taskTestWithTags::log).with().join()

        shouldThrow<FailedException> { deferred.await() }

        client.retry(taskTest, "foo").join()

        delay(50)

        deferred.await() shouldBe "00001"
    }

    "Task canceled during automatic retry" {
        TaskTestImpl.behavior = { _, _ -> Status.FAILED_WITH_RETRY }

        val deferred = client.start(taskTest::log).with().join()

        later { client.cancel(taskTest, deferred.id) }

        shouldThrow<CanceledException> { deferred.await() }
    }

    "Task canceled using A tag" {
        TaskTestImpl.behavior = { _, _ -> Status.FAILED_WITH_RETRY }

        val deferred = client.start(taskTestWithTags::log).with().join()

        later { client.cancel(taskTest, "foo") }

        shouldThrow<CanceledException> { deferred.await() }
    }

    "2 Task canceled using A tag" {
        TaskTestImpl.behavior = { _, _ -> Status.FAILED_WITH_RETRY }

        val deferred1 = client.start(taskTestWithTags::log).with().join()
        val deferred2 = client.start(taskTestWithTags::log).with().join()

        later { client.cancel(taskTest, "foo") }

        later(0) {
            launch { shouldThrow<CanceledException> { deferred1.await() } }
            launch { shouldThrow<CanceledException> { deferred2.await() } }
        }.join()
    }

    "Tag should be added then deleted after completion" {
        TaskTestImpl.behavior = { _, _ -> Status.SUCCESS }

        val deferred = client.start(taskTestWithTags::await).with(200).join()

        client.getIds(taskTest, "foo").contains(deferred.id) shouldBe true
        client.getIds(taskTest, "bar").contains(deferred.id) shouldBe true

        deferred.await()

        // wait a bit to ensure tag propagation
        delay(200)

        client.getIds(taskTest, "foo").contains(deferred.id) shouldBe false
        client.getIds(taskTest, "bar").contains(deferred.id) shouldBe false
    }

    "Tag should be added then deleted after cancellation" {
        TaskTestImpl.behavior = { _, _ -> Status.FAILED_WITH_RETRY }

        val deferred = client.start(taskTestWithTags::log).with().join()

        client.getIds(taskTest, "foo").contains(deferred.id) shouldBe true
        client.getIds(taskTest, "bar").contains(deferred.id) shouldBe true

        later { client.cancel(taskTest, deferred.id) }

        shouldThrow<CanceledException> { deferred.await() }

        // wait a bit to ensure tag propagation
        delay(200)
        client.getIds(taskTest, "foo").contains(deferred.id) shouldBe false
        client.getIds(taskTest, "bar").contains(deferred.id) shouldBe false
    }

    "get tags from context" {
        val taskWithTags = client.taskStub(TaskA::class.java, tags = setOf("foo", "bar"))

        taskWithTags.tags() shouldBe setOf("foo", "bar")
    }

    "get meta from context" {
        val taskWithMeta = client.taskStub(TaskA::class.java, meta = mapOf("foo" to "bar".toByteArray()))

        taskWithMeta.meta() shouldBe TaskMeta(mapOf("foo" to "bar".toByteArray()))
    }
})
