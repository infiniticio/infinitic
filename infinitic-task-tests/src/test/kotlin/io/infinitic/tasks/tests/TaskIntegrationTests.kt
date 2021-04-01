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
import io.infinitic.client.output.FunctionsClientOutput
import io.infinitic.common.clients.data.ClientName
import io.infinitic.common.clients.messages.ClientResponseMessage
import io.infinitic.common.data.MillisDuration
import io.infinitic.common.monitoring.global.messages.MonitoringGlobalMessage
import io.infinitic.common.monitoring.perName.messages.MonitoringPerNameMessage
import io.infinitic.common.monitoring.perName.messages.TaskStatusUpdated
import io.infinitic.common.tags.messages.TagEngineMessage
import io.infinitic.common.tasks.data.TaskId
import io.infinitic.common.tasks.data.TaskStatus
import io.infinitic.common.tasks.engine.messages.TaskEngineMessage
import io.infinitic.common.tasks.engine.transport.SendToTaskEngine
import io.infinitic.common.tasks.executors.messages.TaskExecutorMessage
import io.infinitic.common.workflows.engine.messages.WorkflowEngineMessage
import io.infinitic.monitoring.global.engine.MonitoringGlobalEngine
import io.infinitic.monitoring.global.engine.storage.BinaryMonitoringGlobalStateStorage
import io.infinitic.monitoring.perName.engine.MonitoringPerNameEngine
import io.infinitic.monitoring.perName.engine.output.FunctionsMonitoringPerNameOutput
import io.infinitic.monitoring.perName.engine.storage.BinaryMonitoringPerNameStateStorage
import io.infinitic.storage.inMemory.keySet.InMemoryKeySetStorage
import io.infinitic.storage.inMemory.keyValue.InMemoryKeyValueStorage
import io.infinitic.tags.engine.TagEngine
import io.infinitic.tags.engine.output.FunctionsTagEngineOutput
import io.infinitic.tags.engine.storage.BinaryTagStateStorage
import io.infinitic.tasks.engine.TaskEngine
import io.infinitic.tasks.engine.output.FunctionsTaskEngineOutput
import io.infinitic.tasks.engine.storage.states.BinaryTaskStateStorage
import io.infinitic.tasks.executor.TaskExecutor
import io.infinitic.tasks.executor.register.TaskExecutorRegisterImpl
import io.infinitic.tasks.executor.transport.TaskExecutorOutput
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
import java.util.UUID

private var taskStatus: TaskStatus? = null
private val taskTest = TaskTestImpl()
val keyValueStorage = InMemoryKeyValueStorage()
val keySetStorage = InMemoryKeySetStorage()
private val tagStateStorage = BinaryTagStateStorage(keyValueStorage, keySetStorage)
private val taskStateStorage = BinaryTaskStateStorage(keyValueStorage)
private val monitoringPerNameStateStorage = BinaryMonitoringPerNameStateStorage(keyValueStorage)
private val monitoringGlobalStateStorage = BinaryMonitoringGlobalStateStorage(keyValueStorage)

private lateinit var tagEngine: TagEngine
private lateinit var taskEngine: TaskEngine
private lateinit var monitoringPerNameEngine: MonitoringPerNameEngine
private lateinit var monitoringGlobalEngine: MonitoringGlobalEngine
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
            taskId = TaskId(client.async(taskStub) { log() })
        }
        // check that task is terminated
        taskStateStorage.getState(taskId) shouldBe null
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
            taskId = TaskId(client.async(taskStub) { log() })
        }
        // check that task is terminated
        taskStateStorage.getState(taskId) shouldBe null
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
            taskId = TaskId(client.async(taskStub) { log() })
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
            taskId = TaskId(client.async(taskStub) { log() })
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
            id = client.async(taskStub) { log() }

            val existingTask = client.getTask<TaskTest>(id)
            while (taskStatus != TaskStatus.RUNNING_ERROR) {
                delay(50)
            }
            client.retry(existingTask)
        }
        // check that task is terminated
        taskStateStorage.getState(TaskId(id)) shouldBe null
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
        var id: UUID
        coroutineScope {
            init()
            id = client.async(taskStub) { log() }

            while (taskStatus != TaskStatus.RUNNING_ERROR) {
                delay(50)
            }
            client.retry(taskStub)
        }
        // check that task is terminated
        taskStateStorage.getState(TaskId(id)) shouldBe null
        // check that task is completed
        taskStatus shouldBe TaskStatus.TERMINATED_COMPLETED
        // checks number of task processing
        taskTest.log shouldBe "0000001"
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
            val id = client.async(taskStub1Tag) { log() }
            taskId = TaskId(id)
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
        // task will succeed only at the 4th try
        taskTest.behavior = { _, _ -> Status.FAILED_WITH_RETRY }
        // run system
        var id: UUID
        coroutineScope {
            init()
            id = client.async(taskStub) { log() }
            val existingTask = client.getTask(TaskTest::class.java, id)
            delay(100)
            client.cancel(existingTask)
        }
        // check that task is terminated
        taskStateStorage.getState(TaskId(id)) shouldBe null
        // check that task is completed
        taskStatus shouldBe TaskStatus.TERMINATED_CANCELED
    }

    "Multiple Tasks canceled using tags" {
        var taskId1: TaskId
        var taskId2: TaskId
        // task will succeed only at the 4th try
        taskTest.behavior = { _, _ -> Status.FAILED_WITH_RETRY }
        // run system
        coroutineScope {
            init()
            val id1 = client.async(taskStub1Tag) { log() }
            val id2 = client.async(taskStub2Tag) { log() }
            taskId1 = TaskId(id1)
            taskId2 = TaskId(id2)
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

    "Synchronous Task succeeds at first try" {
        // task will succeed
        taskTest.behavior = { _, _ -> Status.SUCCESS }

        var r: String
        // run system
        coroutineScope {
            init()
            r = taskStub.log()
        }
        // checks number of task processing
        r shouldBe "1"
    }
})

class TestTaskExecutorOutput(private val scope: CoroutineScope) : TaskExecutorOutput {

    override val sendToTaskEngineFn: SendToTaskEngine =
        { msg: TaskEngineMessage, after: MillisDuration -> scope.sendToTaskEngine(msg, after) }
}

fun CoroutineScope.sendToClientResponse(msg: ClientResponseMessage) {
    launch {
        client.handle(msg)
    }
}

fun CoroutineScope.sendToTagEngine(msg: TagEngineMessage) {
    launch {
        tagEngine.handle(msg)
    }
}

fun CoroutineScope.sendToTaskEngine(msg: TaskEngineMessage, after: MillisDuration) {
    launch {
        if (after.long > 0) {
            delay(after.long)
        }
        taskEngine.handle(msg)
    }
}

fun CoroutineScope.sendToMonitoringPerName(msg: MonitoringPerNameMessage) {
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
    keyValueStorage.flush()
    keySetStorage.flush()
    taskStatus = null

    client = Client.with(
        FunctionsClientOutput(
            ClientName("client: InMemory"),
            { msg: TagEngineMessage -> sendToTagEngine(msg) },
            { msg: TaskEngineMessage, after: MillisDuration -> sendToTaskEngine(msg, after) },
            { _: WorkflowEngineMessage, _: MillisDuration -> }
        )
    )

    taskStub = client.newTask(TaskTest::class.java)
    taskStub1Tag = client.newTask(TaskTest::class.java, tags = setOf("foo"))
    taskStub2Tag = client.newTask(TaskTest::class.java, tags = setOf("foo", "bar"))

    tagEngine = TagEngine(
        tagStateStorage,
        FunctionsTagEngineOutput(
            { msg: ClientResponseMessage -> sendToClientResponse(msg) },
            { msg: TaskEngineMessage, after: MillisDuration -> sendToTaskEngine(msg, after) },
            { _: WorkflowEngineMessage, _: MillisDuration -> }
        )
    )

    taskEngine = TaskEngine(
        taskStateStorage,
        FunctionsTaskEngineOutput(
            { msg: ClientResponseMessage -> sendToClientResponse(msg) },
            { msg: TagEngineMessage -> sendToTagEngine(msg) },
            { msg: TaskEngineMessage, after: MillisDuration -> sendToTaskEngine(msg, after) },
            { _: WorkflowEngineMessage, _: MillisDuration -> },
            { msg: TaskExecutorMessage -> sendToWorkers(msg) },
            { msg: MonitoringPerNameMessage -> sendToMonitoringPerName(msg) }
        )
    )

    monitoringPerNameEngine = MonitoringPerNameEngine(
        monitoringPerNameStateStorage,
        FunctionsMonitoringPerNameOutput(
            { msg: MonitoringGlobalMessage -> sendToMonitoringGlobal(msg) }
        )
    )

    monitoringGlobalEngine = MonitoringGlobalEngine(monitoringGlobalStateStorage)

    taskExecutor = TaskExecutor(TestTaskExecutorOutput(this), TaskExecutorRegisterImpl())
    taskExecutor.register(TaskTest::class.java.name) { taskTest }
}
