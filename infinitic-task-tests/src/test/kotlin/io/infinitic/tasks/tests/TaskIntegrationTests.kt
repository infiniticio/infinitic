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
import io.infinitic.common.monitoring.global.messages.MonitoringGlobalMessage
import io.infinitic.common.monitoring.perName.messages.MonitoringPerNameEngineMessage
import io.infinitic.common.monitoring.perName.messages.TaskStatusUpdated
import io.infinitic.common.tasks.data.TaskInstance
import io.infinitic.common.tasks.data.TaskStatus
import io.infinitic.common.tasks.engine.messages.TaskEngineMessage
import io.infinitic.common.tasks.executors.messages.TaskExecutorMessage
import io.infinitic.common.workflows.engine.messages.WorkflowEngineMessage
import io.infinitic.monitoring.global.engine.MonitoringGlobalEngine
import io.infinitic.monitoring.global.engine.storage.MonitoringGlobalStateKeyValueStorage
import io.infinitic.monitoring.perName.engine.MonitoringPerNameEngine
import io.infinitic.monitoring.perName.engine.storage.MonitoringPerNameStateKeyValueStorage
import io.infinitic.storage.inMemory.InMemoryStorage
import io.infinitic.tasks.engine.TaskEngine
import io.infinitic.tasks.engine.storage.TaskStateKeyValueStorage
import io.infinitic.tasks.executor.TaskExecutor
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

private var taskStatus: TaskStatus? = null
private val taskTest = TaskTestImpl()

private val taskStateStorage = TaskStateKeyValueStorage(InMemoryStorage())
private val monitoringPerNameStateStorage = MonitoringPerNameStateKeyValueStorage(InMemoryStorage())
private val monitoringGlobalStateStorage = MonitoringGlobalStateKeyValueStorage(InMemoryStorage())

private lateinit var taskEngine: TaskEngine
private lateinit var monitoringPerNameEngine: MonitoringPerNameEngine
private lateinit var monitoringGlobalEngine: MonitoringGlobalEngine
private lateinit var taskExecutor: TaskExecutor
private lateinit var client: Client

fun CoroutineScope.sendToTaskEngine(msg: TaskEngineMessage, after: Float) {
    launch {
        if (after > 0F) {
            delay((1000 * after).toLong())
        }
        taskEngine.handle(msg)
    }
}

fun CoroutineScope.sendToMonitoringPerName(msg: MonitoringPerNameEngineMessage) {
    launch {
        monitoringPerNameEngine.handle(msg)

        // catch status update
        if (msg is TaskStatusUpdated) {
            taskStatus = msg.newStatus
        }
    }
}

fun CoroutineScope.sendToMonitoringGlobal(msg: MonitoringGlobalMessage) {
    launch {
        monitoringGlobalEngine.handle(msg)
    }
}

fun CoroutineScope.sendToWorkers(msg: TaskExecutorMessage) {
    launch {
        taskExecutor.handle(msg)
    }
}

fun CoroutineScope.init() {
    taskStateStorage.flush()
    monitoringPerNameStateStorage.flush()
    monitoringGlobalStateStorage.flush()
    taskStatus = null

    client = Client(
        { msg: TaskEngineMessage, _: Float -> sendToTaskEngine(msg, 0F) },
        { _: WorkflowEngineMessage, _: Float -> }
    )

    taskEngine = TaskEngine(
        taskStateStorage,
        { },
        { msg: TaskEngineMessage, after: Float -> sendToTaskEngine(msg, after) },
        { msg: MonitoringPerNameEngineMessage -> sendToMonitoringPerName(msg) },
        { msg: TaskExecutorMessage -> sendToWorkers(msg) },
        { _: WorkflowEngineMessage, _: Float -> }
    )

    monitoringPerNameEngine = MonitoringPerNameEngine(monitoringPerNameStateStorage) {
        msg: MonitoringGlobalMessage ->
        sendToMonitoringGlobal(msg)
    }

    monitoringGlobalEngine = MonitoringGlobalEngine(monitoringGlobalStateStorage)

    taskExecutor = TaskExecutor { msg: TaskEngineMessage, after: Float -> sendToTaskEngine(msg, after) }
    taskExecutor.register(TaskTest::class.java.name) { taskTest }
}

class TaskIntegrationTests : StringSpec({
    var task: TaskInstance

    "Task succeeds at first try" {
        // task will succeed
        taskTest.behavior = { _, _ -> Status.SUCCESS }
        // run system
        coroutineScope {
            init()
            task = client.dispatch<TaskTest> { log() }
        }
        // check that task is terminated
        taskStateStorage.getState(task.taskId) shouldBe null
        // check that task is completed
        taskStatus shouldBe TaskStatus.TERMINATED_COMPLETED
        // checks number of task processing
        taskTest.log shouldBe "1"
    }

    "Task succeeds at 4th try" {
        // task will succeed only at the 4th try
        taskTest.behavior = { _, retry -> if (retry < 3) Status.FAILED_WITH_RETRY else Status.SUCCESS }
        // run system
        coroutineScope {
            init()
            task = client.dispatch<TaskTest> { log() }
        }
        // check that task is terminated
        taskStateStorage.getState(task.taskId) shouldBe null
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
            task = client.dispatch<TaskTest> { log() }
        }
        // check that task is not terminated
        taskStateStorage.getState(task.taskId) shouldNotBe null
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
            task = client.dispatch<TaskTest> { log() }
        }
        // check that task is not terminated
        taskStateStorage.getState(task.taskId) shouldNotBe null
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
        coroutineScope {
            init()
            task = client.dispatch<TaskTest> { log() }
            while (taskStatus != TaskStatus.RUNNING_ERROR) {
                delay(50)
            }
            client.retryTask(id = "${task.taskId}")
        }
        // check that task is terminated
        taskStateStorage.getState(task.taskId) shouldBe null
        // check that task is completed
        taskStatus shouldBe TaskStatus.TERMINATED_COMPLETED
        // checks number of task processing
        taskTest.log shouldBe "0000001"
    }

    "Task canceled during automatic retry" {
        // task will succeed only at the 4th try
        taskTest.behavior = { _, _ -> Status.FAILED_WITH_RETRY }
        // run system
        // run system
        coroutineScope {
            init()
            task = client.dispatch<TaskTest> { log() }
            delay(100)
            client.cancelTask(id = "${task.taskId}")
        }
        // check that task is terminated
        taskStateStorage.getState(task.taskId) shouldBe null
        // check that task is completed
        taskStatus shouldBe TaskStatus.TERMINATED_CANCELED
    }
})
