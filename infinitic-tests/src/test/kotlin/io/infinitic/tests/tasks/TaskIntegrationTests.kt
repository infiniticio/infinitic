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

import io.infinitic.client.Client
import io.infinitic.common.tasks.data.TaskInstance
import io.infinitic.common.tasks.data.TaskStatus
import io.infinitic.common.tasks.messages.ForMonitoringGlobalMessage
import io.infinitic.common.tasks.messages.ForMonitoringPerNameMessage
import io.infinitic.common.tasks.messages.ForTaskEngineMessage
import io.infinitic.common.tasks.messages.ForWorkerMessage
import io.infinitic.common.workflows.messages.ForWorkflowEngineMessage
import io.infinitic.engine.taskManager.engines.MonitoringGlobal
import io.infinitic.engine.taskManager.engines.MonitoringPerName
import io.infinitic.engine.taskManager.engines.TaskEngine
import io.infinitic.tests.tasks.inMemory.InMemoryReceiverTest
import io.infinitic.tests.tasks.samples.Status
import io.infinitic.tests.tasks.samples.TaskTest
import io.infinitic.tests.tasks.samples.TaskTestImpl
import io.infinitic.tests.tasks.inMemory.InMemoryEmetter
import io.infinitic.tests.tasks.inMemory.InMemoryReceiver
import io.infinitic.tests.tasks.inMemory.InMemoryStorageTest
import io.infinitic.worker.Worker
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay

private val storage = InMemoryStorageTest()
private val forWorkflowEngineTransport = Channel<ForWorkflowEngineMessage>(64)
private val forTaskEngineTransport = Channel<ForTaskEngineMessage>(64)
private val forMonitoringPerNameTransport = Channel<ForMonitoringPerNameMessage>(64)
private val forMonitoringGlobalTransport = Channel<ForMonitoringGlobalMessage>(64)
private val forWorkerTransport = Channel<ForWorkerMessage>(64)
private val taskEngineReceiver = InMemoryReceiverTest(forTaskEngineTransport)

private val client = Client(
    InMemoryEmetter(forTaskEngineTransport),
    InMemoryEmetter(forWorkflowEngineTransport)
)
private val taskEngine = TaskEngine(
    storage,
    taskEngineReceiver,
    InMemoryEmetter(forTaskEngineTransport),
    InMemoryEmetter(forMonitoringPerNameTransport),
    InMemoryEmetter(forWorkerTransport)
)
private val monitoringPerNameEngine = MonitoringPerName(
    storage,
    InMemoryReceiver(forMonitoringPerNameTransport),
    InMemoryEmetter(forMonitoringGlobalTransport),
)
private val monitoringGlobalEngine = MonitoringGlobal(
    storage,
    InMemoryReceiver(forMonitoringGlobalTransport)
)
private val worker = Worker(
    InMemoryReceiver(forWorkerTransport),
    InMemoryEmetter(forTaskEngineTransport)
)

class TaskIntegrationTests : StringSpec({
    val taskTest = TaskTestImpl()
    worker.register(TaskTest::class.java.name) { taskTest }
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
            listen(this)
            task = client.dispatch(TaskTest::class.java) { log() }
        }
        // check that task is terminated
        storage.isTerminated(task) shouldBe true
        // check that task is completed
        taskEngineReceiver.taskStatus shouldBe TaskStatus.TERMINATED_COMPLETED
        // checks number of task processing
        TaskTestImpl.log shouldBe "1"
    }

    "Task succeeds at 4th try" {
        // task will succeed only at the 4th try
        taskTest.behavior = { _, retry -> if (retry < 3) Status.FAILED_WITH_RETRY else Status.SUCCESS }
        // run system
        coroutineScope {
            listen(this)
            task = client.dispatch(TaskTest::class.java) { log() }
        }
        // check that task is terminated
        storage.isTerminated(task) shouldBe true
        // check that task is completed
        taskEngineReceiver.taskStatus shouldBe TaskStatus.TERMINATED_COMPLETED
        // checks number of task processing
        TaskTestImpl.log shouldBe "0001"
    }

    "Task fails at first try" {
        // task will succeed only at the 4th try
        taskTest.behavior = { _, _ -> Status.FAILED_WITHOUT_RETRY }
        // run system
        coroutineScope {
            listen(this)
            task = client.dispatch(TaskTest::class.java) { log() }
        }
        // check that task is not terminated
        storage.isTerminated(task) shouldBe false
        // check that task is failed
        taskEngineReceiver.taskStatus shouldBe TaskStatus.RUNNING_ERROR
        // checks number of task processing
        TaskTestImpl.log shouldBe "0"
    }

    "Task fails after 4 tries " {
        // task will succeed only at the 4th try
        taskTest.behavior = { _, retry -> if (retry < 3) Status.FAILED_WITH_RETRY else Status.FAILED_WITHOUT_RETRY }
        // run system
        coroutineScope {
            listen(this)
            task = client.dispatch(TaskTest::class.java) { log() }
        }
        // check that task is not terminated
        storage.isTerminated(task) shouldBe false
        // check that task is failed
        taskEngineReceiver.taskStatus shouldBe TaskStatus.RUNNING_ERROR
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
            listen(this)
            task = client.dispatch(TaskTest::class.java) { log() }
            delay(100)
            client.retryTask(id = "${task.taskId}")
        }
        // check that task is terminated
        storage.isTerminated(task)
        // check that task is completed
        taskEngineReceiver.taskStatus shouldBe TaskStatus.TERMINATED_COMPLETED
        // checks number of task processing
        TaskTestImpl.log shouldBe "0000001"
    }

    "Task canceled during automatic retry" {
        // task will succeed only at the 4th try
        taskTest.behavior = { _, _ -> Status.FAILED_WITH_RETRY }
        // run system
        // run system
        coroutineScope {
            listen(this)
            task = client.dispatch(TaskTest::class.java) { log() }
            delay(100)
            client.cancelTask(id = "${task.taskId}")
        }
        // check that task is terminated
        storage.isTerminated(task)
        // check that task is completed
        taskEngineReceiver.taskStatus shouldBe TaskStatus.TERMINATED_CANCELED
    }
})

suspend fun listen(scope: CoroutineScope) {
    taskEngine.listen(scope)
    monitoringPerNameEngine.listen(scope)
    monitoringGlobalEngine.listen(scope)
    worker.listen(scope)
}
