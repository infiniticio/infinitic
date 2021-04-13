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

package io.infinitic.tasks.tests

import io.infinitic.client.Client
import io.infinitic.common.clients.data.ClientName
import io.infinitic.common.clients.messages.ClientMessage
import io.infinitic.common.data.MillisDuration
import io.infinitic.common.metrics.global.messages.MetricsGlobalMessage
import io.infinitic.common.metrics.perName.messages.MetricsPerNameMessage
import io.infinitic.common.metrics.perName.messages.TaskStatusUpdated
import io.infinitic.common.storage.keySet.LoggedKeySetStorage
import io.infinitic.common.storage.keyValue.LoggedKeyValueStorage
import io.infinitic.common.tasks.data.TaskId
import io.infinitic.common.tasks.data.TaskName
import io.infinitic.common.tasks.data.TaskStatus
import io.infinitic.common.tasks.data.TaskTag
import io.infinitic.common.tasks.engine.messages.TaskEngineMessage
import io.infinitic.common.tasks.executors.messages.TaskExecutorMessage
import io.infinitic.common.tasks.tags.messages.TaskTagEngineMessage
import io.infinitic.common.workflows.engine.messages.WorkflowEngineMessage
import io.infinitic.common.workflows.tags.messages.WorkflowTagEngineMessage
import io.infinitic.metrics.global.engine.MetricsGlobalEngine
import io.infinitic.metrics.global.engine.storage.BinaryMetricsGlobalStateStorage
import io.infinitic.metrics.perName.engine.MetricsPerNameEngine
import io.infinitic.metrics.perName.engine.storage.BinaryMetricsPerNameStateStorage
import io.infinitic.storage.inMemory.InMemoryKeySetStorage
import io.infinitic.storage.inMemory.InMemoryKeyValueStorage
import io.infinitic.tags.tasks.TaskTagEngine
import io.infinitic.tags.tasks.storage.BinaryTaskTagStorage
import io.infinitic.tasks.engine.TaskEngine
import io.infinitic.tasks.engine.storage.BinaryTaskStateStorage
import io.infinitic.tasks.executor.TaskExecutor
import io.infinitic.tasks.executor.register.TaskExecutorRegisterImpl
import io.infinitic.tasks.tests.samples.Status
import io.infinitic.tasks.tests.samples.TaskTest
import io.infinitic.tasks.tests.samples.TaskTestImpl
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield
import java.util.UUID

private var taskStatus: TaskStatus? = null
private val taskTest = TaskTestImpl()
val keyValueStorage = LoggedKeyValueStorage(InMemoryKeyValueStorage())
val keySetStorage = LoggedKeySetStorage(InMemoryKeySetStorage())
private val taskTagStorage = BinaryTaskTagStorage(keyValueStorage, keySetStorage)
private val taskStateStorage = BinaryTaskStateStorage(keyValueStorage)
private val metricsPerNameStateStorage = BinaryMetricsPerNameStateStorage(keyValueStorage)
private val metricsGlobalStateStorage = BinaryMetricsGlobalStateStorage(keyValueStorage)

private lateinit var taskTagEngine: TaskTagEngine
private lateinit var taskEngine: TaskEngine
private lateinit var metricsPerNameEngine: MetricsPerNameEngine
private lateinit var metricsGlobalEngine: MetricsGlobalEngine
private lateinit var taskExecutor: TaskExecutor
private lateinit var client: Client
private lateinit var taskStub: TaskTest
private lateinit var taskStub1Tag: TaskTest
private lateinit var taskStub2Tag: TaskTest

class TaskIntegrationTests : StringSpec({
    var taskId: TaskId

    "Task succeeds at first try" {
        // task will succeed
        taskTest.behavior = { _, _ -> Status.SUCCESS }
        // run system
        coroutineScope {
            init()
            val deferred = client.async(taskStub) { log() }
            taskId = TaskId(deferred.id)
        }
        // check that task is terminated
        taskStateStorage.getState(taskId) shouldBe null
        // check that task is completed
        taskStatus shouldBe TaskStatus.TERMINATED_COMPLETED
        // checks number of task processing
        taskTest.log shouldBe "1"
    }

    "Synchronous execution succeeds at first try" {
        var result: String? = null
        // task will succeed
        taskTest.behavior = { _, _ -> Status.SUCCESS }
        // run system
        coroutineScope {
            init()
            result = taskStub.log()
        }
        // check that task is completed
        taskStatus shouldBe TaskStatus.TERMINATED_COMPLETED
        // checks number of task processing
        result shouldBe "1"
    }

    "Waiting for asynchronous execution succeeding at first try" {
        var result: String? = null
        // task will succeed
        taskTest.behavior = { _, _ -> Status.SUCCESS }
        // run system
        coroutineScope {
            init()
            val deferred = client.async(taskStub) { log() }
            result = deferred.await()
        }
        // check that task is completed
        taskStatus shouldBe TaskStatus.TERMINATED_COMPLETED
        // checks number of task processing
        result shouldBe "1"
    }

    "Task succeeds at 4th try" {
        // task will succeed only at the 4th try
        taskTest.behavior = { _, retry -> if (retry < 3) Status.FAILED_WITH_RETRY else Status.SUCCESS }
        // run system
        coroutineScope {
            init()
            val deferred = client.async(taskStub) { log() }
            taskId = TaskId(deferred.id)
        }
        // check that task is terminated
        taskStateStorage.getState(taskId) shouldBe null
        // check that task is completed
        taskStatus shouldBe TaskStatus.TERMINATED_COMPLETED
        // checks number of task processing
        taskTest.log shouldBe "0001"
    }

    "Task succeeds synchronously at 4th try" {
        var result: String? = null
        // task will succeed only at the 4th try
        taskTest.behavior = { _, retry -> if (retry < 3) Status.FAILED_WITH_RETRY else Status.SUCCESS }
        // run system
        coroutineScope {
            init()
            result = taskStub.log()
        }
        // check that task is completed
        taskStatus shouldBe TaskStatus.TERMINATED_COMPLETED
        // checks number of task processing
        taskTest.log shouldBe "0001"
    }

    "Task succeeds asynchronously at 4th try" {
        var result: String? = null
        // task will succeed only at the 4th try
        taskTest.behavior = { _, retry -> if (retry < 3) Status.FAILED_WITH_RETRY else Status.SUCCESS }
        // run system
        coroutineScope {
            init()
            val deferred = client.async(taskStub) { log() }
            taskId = TaskId(deferred.id)
        }
        // check that task is completed
        taskStatus shouldBe TaskStatus.TERMINATED_COMPLETED
        // checks number of task processing
        taskTest.log shouldBe "0001"
    }

    "Task fails at first try" {
        // task will succeed only at the 4th try
        taskTest.behavior = { _, _ -> Status.FAILED_WITHOUT_RETRY }
        // run system
        coroutineScope {
            init()
            val deferred = client.async(taskStub) { log() }
            taskId = TaskId(deferred.id)
        }
        // check that task is not terminated
        taskStateStorage.getState(taskId) shouldNotBe null
        // check that task is failed
        taskStatus shouldBe TaskStatus.RUNNING_ERROR
        // checks number of task processing
        taskTest.log shouldBe "0"
    }

    "Task fails after 4 tries " {
        // task will succeed only at the 4th try
        taskTest.behavior = { _, retry -> if (retry < 3) Status.FAILED_WITH_RETRY else Status.FAILED_WITHOUT_RETRY }
        // run system
        coroutineScope {
            init()
            val deferred = client.async(taskStub) { log() }
            taskId = TaskId(deferred.id)
        }
        // check that task is not terminated
        taskStateStorage.getState(taskId) shouldNotBe null
        // check that task is failed
        taskStatus shouldBe TaskStatus.RUNNING_ERROR
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
        var id: UUID
        coroutineScope {
            init()
            val deferred = client.async(taskStub) { log() }
            taskId = TaskId(deferred.id)

            val existingTask = client.getTask<TaskTest>(deferred.id)
            while (taskStatus != TaskStatus.RUNNING_ERROR) {
                delay(50)
            }
            client.retry(existingTask)
        }
        // check that task is terminated
        taskStateStorage.getState(taskId) shouldBe null
        // check that task is completed
        taskStatus shouldBe TaskStatus.TERMINATED_COMPLETED
        // checks number of task processing
        taskTest.log shouldBe "0000001"
    }

    "Task succeeds after manual retry on same stub" {
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
            init()
            val deferred = client.async(taskStub) { log() }
            taskId = TaskId(deferred.id)

            while (taskStatus != TaskStatus.RUNNING_ERROR) {
                delay(50)
            }
            client.retry(taskStub)
        }
        // check that task is terminated
        taskStateStorage.getState(taskId) shouldBe null
        // check that task is completed
        taskStatus shouldBe TaskStatus.TERMINATED_COMPLETED
        // checks number of task processing (meta has been reinitialized at retry)
        taskTest.log shouldBe "001"
    }

    "Task succeeds after manual retry using tag" {
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
            init()
            val deferred = client.async(taskStub1Tag) { log() }
            taskId = TaskId(deferred.id)
            val existingTask = client.getTask<TaskTest>("foo")
            while (taskStatus != TaskStatus.RUNNING_ERROR) {
                delay(50)
            }
            client.retry(existingTask)
        }
        // check that task is terminated
        taskStateStorage.getState(taskId) shouldBe null
        // check that task is completed
        taskStatus shouldBe TaskStatus.TERMINATED_COMPLETED
        // checks number of task processing
        taskTest.log shouldBe "0000001"
    }

    "Task canceled during automatic retry" {
        // task will fail and retry
        taskTest.behavior = { _, _ -> Status.FAILED_WITH_RETRY }
        // run system
        var id: UUID
        coroutineScope {
            init()
            val deferred = client.async(taskStub) { log() }
            taskId = TaskId(deferred.id)
            val existingTask = client.getTask(TaskTest::class.java, deferred.id)
            delay(100)
            client.cancel(existingTask)
        }
        // check that task is terminated
        taskStateStorage.getState(taskId) shouldBe null
        // check that task is completed
        taskStatus shouldBe TaskStatus.TERMINATED_CANCELED
    }

    "Multiple Tasks canceled using tags" {
        var taskId1: TaskId
        var taskId2: TaskId
        // task will fail and retry
        taskTest.behavior = { _, _ -> Status.FAILED_WITH_RETRY }
        // run system
        coroutineScope {
            init()
            val deferred1 = client.async(taskStub1Tag) { log() }
            val deferred2 = client.async(taskStub2Tag) { log() }
            taskId1 = TaskId(deferred1.id)
            taskId2 = TaskId(deferred2.id)
            val existingTask = client.getTask(TaskTest::class.java, "foo")
            delay(100)
            client.cancel(existingTask)
        }
        // check that task is terminated
        taskStateStorage.getState(taskId1) shouldBe null
        taskStateStorage.getState(taskId2) shouldBe null
        // check that task is completed
        taskStatus shouldBe TaskStatus.TERMINATED_CANCELED
    }

    "Tag should be added then deleted after completion" {
        // task will succeed
        taskTest.behavior = { _, _ -> Status.SUCCESS }
        // run system
        coroutineScope {
            init()
            val deferred = client.async(taskStub2Tag) { await(100) }
            taskId = TaskId(deferred.id)
            yield()
            // checks taskId has been added to tag storage
            taskTagStorage.getTaskIds(TaskTag("foo"), TaskName(TaskTest::class.java.name)).contains(taskId) shouldBe true
            taskTagStorage.getTaskIds(TaskTag("bar"), TaskName(TaskTest::class.java.name)).contains(taskId) shouldBe true
        }
        // check that task is terminated
        taskStateStorage.getState(taskId) shouldBe null
        // check that task is completed
        taskStatus shouldBe TaskStatus.TERMINATED_COMPLETED
        // checks taskId has been removed from tag storage
        taskTagStorage.getTaskIds(TaskTag("foo"), TaskName(TaskTest::class.java.name)).contains(taskId) shouldBe false
        taskTagStorage.getTaskIds(TaskTag("bar"), TaskName(TaskTest::class.java.name)).contains(taskId) shouldBe false
    }

    "Tag should be added then deleted after cancellation" {
        // task will fail and retry
        taskTest.behavior = { _, _ -> Status.FAILED_WITH_RETRY }
        // run system
        coroutineScope {
            init()
            val deferred = client.async(taskStub2Tag) { log() }
            taskId = TaskId(deferred.id)

            yield()
            // checks taskId has been added to tag storage
            taskTagStorage.getTaskIds(TaskTag("foo"), TaskName(TaskTest::class.java.name)).contains(taskId) shouldBe true
            taskTagStorage.getTaskIds(TaskTag("bar"), TaskName(TaskTest::class.java.name)).contains(taskId) shouldBe true
            client.cancel(taskStub2Tag)
        }
        // check that task is terminated
        taskStateStorage.getState(taskId) shouldBe null
        // check that task is completed
        taskStatus shouldBe TaskStatus.TERMINATED_CANCELED
        // checks taskId has been removed from tag storage
        taskTagStorage.getTaskIds(TaskTag("foo"), TaskName(TaskTest::class.java.name)).contains(taskId) shouldBe false
        taskTagStorage.getTaskIds(TaskTag("bar"), TaskName(TaskTest::class.java.name)).contains(taskId) shouldBe false
    }
})

fun CoroutineScope.sendToClientResponse(msg: ClientMessage) = launch {
    client.handle(msg)
}

fun CoroutineScope.sendToTaskTagEngine(msg: TaskTagEngineMessage) = launch {
    taskTagEngine.handle(msg)
}

fun CoroutineScope.sendToTaskEngine(msg: TaskEngineMessage, after: MillisDuration) = launch {
    if (after.long > 0) {
        delay(after.long)
    }
    taskEngine.handle(msg)
}

fun CoroutineScope.sendToMetricsPerName(msg: MetricsPerNameMessage) = launch {
    metricsPerNameEngine.handle(msg)

    // catch status update
    if (msg is TaskStatusUpdated) {
        taskStatus = msg.newStatus
    }
}

fun CoroutineScope.sendToMetricsGlobal(msg: MetricsGlobalMessage) = launch {
    metricsGlobalEngine.handle(msg)
}

fun CoroutineScope.sendToWorkers(msg: TaskExecutorMessage) = launch {
    taskExecutor.handle(msg)
}

fun CoroutineScope.init() {
    keyValueStorage.flush()
    keySetStorage.flush()
    taskStatus = null

    client = Client.with(
        ClientName("client: InMemory"),
        { msg: TaskTagEngineMessage -> sendToTaskTagEngine(msg) },
        { msg: TaskEngineMessage, after: MillisDuration -> sendToTaskEngine(msg, after) },
        { _: WorkflowTagEngineMessage -> },
        { _: WorkflowEngineMessage, _: MillisDuration -> }
    )

    taskStub = client.newTask(TaskTest::class.java)
    taskStub1Tag = client.newTask(TaskTest::class.java, tags = setOf("foo"))
    taskStub2Tag = client.newTask(TaskTest::class.java, tags = setOf("foo", "bar"))

    taskTagEngine = TaskTagEngine(
        taskTagStorage,
        { msg: TaskEngineMessage, after: MillisDuration -> sendToTaskEngine(msg, after) }
    )

    taskEngine = TaskEngine(
        taskStateStorage,
        { msg: ClientMessage -> sendToClientResponse(msg) },
        { msg: TaskTagEngineMessage -> sendToTaskTagEngine(msg) },
        { msg: TaskEngineMessage, after: MillisDuration -> sendToTaskEngine(msg, after) },
        { _: WorkflowEngineMessage, _: MillisDuration -> },
        { msg: TaskExecutorMessage -> sendToWorkers(msg) },
        { msg: MetricsPerNameMessage -> sendToMetricsPerName(msg) }
    )

    metricsPerNameEngine = MetricsPerNameEngine(
        metricsPerNameStateStorage,
        { msg: MetricsGlobalMessage -> sendToMetricsGlobal(msg) }
    )

    metricsGlobalEngine = MetricsGlobalEngine(metricsGlobalStateStorage)

    taskExecutor = TaskExecutor(
        { msg: TaskEngineMessage, after: MillisDuration -> sendToTaskEngine(msg, after) },
        TaskExecutorRegisterImpl()
    )
    taskExecutor.registerTask(TaskTest::class.java.name) { taskTest }
}
