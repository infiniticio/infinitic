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

import io.infinitic.cache.no.NoCache
import io.infinitic.client.Client
import io.infinitic.client.transport.ClientOutput
import io.infinitic.common.clients.data.ClientName
import io.infinitic.common.clients.messages.ClientResponseMessage
import io.infinitic.common.clients.transport.SendToClientResponse
import io.infinitic.common.data.MillisDuration
import io.infinitic.common.monitoring.global.messages.MonitoringGlobalMessage
import io.infinitic.common.monitoring.global.transport.SendToMonitoringGlobal
import io.infinitic.common.monitoring.perName.messages.MonitoringPerNameMessage
import io.infinitic.common.monitoring.perName.messages.TaskStatusUpdated
import io.infinitic.common.monitoring.perName.transport.SendToMonitoringPerName
import io.infinitic.common.tags.data.Tag
import io.infinitic.common.tags.messages.TagEngineMessage
import io.infinitic.common.tags.transport.SendToTagEngine
import io.infinitic.common.tasks.data.TaskId
import io.infinitic.common.tasks.data.TaskStatus
import io.infinitic.common.tasks.engine.messages.TaskEngineMessage
import io.infinitic.common.tasks.engine.transport.SendToTaskEngine
import io.infinitic.common.tasks.executors.SendToTaskExecutors
import io.infinitic.common.tasks.executors.messages.TaskExecutorMessage
import io.infinitic.common.workflows.engine.messages.WorkflowEngineMessage
import io.infinitic.common.workflows.engine.transport.SendToWorkflowEngine
import io.infinitic.monitoring.global.engine.MonitoringGlobalEngine
import io.infinitic.monitoring.global.engine.storage.MonitoringGlobalStateKeyValueStorage
import io.infinitic.monitoring.perName.engine.MonitoringPerNameEngine
import io.infinitic.monitoring.perName.engine.storage.MonitoringPerNameStateKeyValueStorage
import io.infinitic.monitoring.perName.engine.transport.MonitoringPerNameOutput
import io.infinitic.storage.inMemory.InMemoryKeySetStorage
import io.infinitic.storage.inMemory.InMemoryKeyValueStorage
import io.infinitic.tags.engine.TagEngine
import io.infinitic.tags.engine.storage.TagStateCachedKeyStorage
import io.infinitic.tags.engine.transport.TagEngineOutput
import io.infinitic.tasks.engine.TaskEngine
import io.infinitic.tasks.engine.storage.events.NoTaskEventStorage
import io.infinitic.tasks.engine.storage.states.TaskStateCachedKeyStorage
import io.infinitic.tasks.engine.transport.TaskEngineOutput
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

private var taskStatus: TaskStatus? = null
private val taskTest = TaskTestImpl()

private val tagStateStorage = TagStateCachedKeyStorage(
    InMemoryKeyValueStorage(),
    NoCache(),
    InMemoryKeySetStorage(),
    NoCache()
)
private val taskStateStorage = TaskStateCachedKeyStorage(InMemoryKeyValueStorage(), NoCache())
private val monitoringPerNameStateStorage = MonitoringPerNameStateKeyValueStorage(InMemoryKeyValueStorage(), NoCache())
private val monitoringGlobalStateStorage = MonitoringGlobalStateKeyValueStorage(InMemoryKeyValueStorage(), NoCache())

private lateinit var tagEngine: TagEngine
private lateinit var taskEngine: TaskEngine
private lateinit var monitoringPerNameEngine: MonitoringPerNameEngine
private lateinit var monitoringGlobalEngine: MonitoringGlobalEngine
private lateinit var taskExecutor: TaskExecutor
private lateinit var client: Client
private lateinit var taskTestStub: TaskTest

class TaskIntegrationTests : StringSpec({
    var taskId: TaskId

    "Task succeeds at first try" {
        // task will succeed
        taskTest.behavior = { _, _ -> Status.SUCCESS }
        // run system
        coroutineScope {
            init()
            taskId = TaskId(client.async(taskTestStub) { log() })
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
            taskId = TaskId(client.async(taskTestStub) { log() })
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
            taskId = TaskId(client.async(taskTestStub) { log() })
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
            taskId = TaskId(client.async(taskTestStub) { log() })
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
        coroutineScope {
            init()
            val id = client.async(taskTestStub) { log() }
            taskId = TaskId(id)
            val existingTask = client.task<TaskTest>("${Tag.of(id)}")
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
        // run system
        coroutineScope {
            init()
            val id = client.async(taskTestStub) { log() }
            taskId = TaskId(id)
            val existingTask = client.task(TaskTest::class.java, "${Tag.of(id)}")
            delay(100)
            client.cancel(existingTask)
        }
        // check that task is terminated
        taskStateStorage.getState(taskId) shouldBe null
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
            r = taskTestStub.log()
        }
        // checks number of task processing
        r shouldBe "1"
    }
})

class TestTagEngineOutput(private val scope: CoroutineScope) : TagEngineOutput {
    override val sendToClientResponseFn: SendToClientResponse =
        { msg: ClientResponseMessage -> scope.sendToClientResponse(msg) }

    override val sendToTaskEngineFn: SendToTaskEngine =
        { msg: TaskEngineMessage, after: MillisDuration -> scope.sendToTaskEngine(msg, after) }

    override val sendToWorkflowEngineFn: SendToWorkflowEngine =
        { _: WorkflowEngineMessage, _: MillisDuration -> }
}

class TestTaskEngineOutput(private val scope: CoroutineScope) : TaskEngineOutput {
    override val sendToClientResponseFn: SendToClientResponse =
        { msg: ClientResponseMessage -> scope.sendToClientResponse(msg) }

    override val sendToTagEngineFn: SendToTagEngine =
        { msg: TagEngineMessage -> scope.sendToTagEngine(msg) }

    override val sendToTaskEngineFn: SendToTaskEngine =
        { msg: TaskEngineMessage, after: MillisDuration -> scope.sendToTaskEngine(msg, after) }

    override val sendToWorkflowEngineFn: SendToWorkflowEngine =
        { _: WorkflowEngineMessage, _: MillisDuration -> }

    override val sendToTaskExecutorsFn: SendToTaskExecutors =
        { msg: TaskExecutorMessage -> scope.sendToWorkers(msg) }

    override val sendToMonitoringPerNameFn: SendToMonitoringPerName =
        { msg: MonitoringPerNameMessage -> scope.sendToMonitoringPerName(msg) }
}

class TestMonitoringPerNameOutput(private val scope: CoroutineScope) : MonitoringPerNameOutput {

    override val sendToMonitoringGlobalFn: SendToMonitoringGlobal =
        { msg: MonitoringGlobalMessage -> scope.sendToMonitoringGlobal(msg) }
}

class TestTaskExecutorOutput(private val scope: CoroutineScope) : TaskExecutorOutput {

    override val sendToTaskEngineFn: SendToTaskEngine =
        { msg: TaskEngineMessage, after: MillisDuration -> scope.sendToTaskEngine(msg, after) }
}

class TestClientOutput(private val scope: CoroutineScope) : ClientOutput {
    override val clientName = ClientName("client: InMemory")

    override val sendToTagEngineFn: SendToTagEngine =
        { msg: TagEngineMessage -> scope.sendToTagEngine(msg) }

    override val sendToTaskEngineFn: SendToTaskEngine =
        { msg: TaskEngineMessage, after: MillisDuration -> scope.sendToTaskEngine(msg, after) }

    override val sendToWorkflowEngineFn: SendToWorkflowEngine = { _: WorkflowEngineMessage, _: MillisDuration -> }
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
    taskStateStorage.flush()
    monitoringPerNameStateStorage.flush()
    monitoringGlobalStateStorage.flush()
    taskStatus = null

    client = Client(TestClientOutput(this))

    taskTestStub = client.task(TaskTest::class.java)

    tagEngine = TagEngine(
        tagStateStorage,
        TestTagEngineOutput(this)
    )

    taskEngine = TaskEngine(
        taskStateStorage,
        NoTaskEventStorage(),
        TestTaskEngineOutput(this)
    )

    monitoringPerNameEngine = MonitoringPerNameEngine(
        monitoringPerNameStateStorage,
        TestMonitoringPerNameOutput(this)
    )

    monitoringGlobalEngine = MonitoringGlobalEngine(monitoringGlobalStateStorage)

    taskExecutor = TaskExecutor(TestTaskExecutorOutput(this), TaskExecutorRegisterImpl())
    taskExecutor.register(TaskTest::class.java.name) { taskTest }
}
