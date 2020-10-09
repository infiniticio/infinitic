package io.infinitic.tests.tasks

//import io.infinitic.engine.taskManager.engines.TaskEngine
import io.infinitic.client.Client
import io.infinitic.common.tasks.messages.ForMonitoringGlobalMessage
import io.infinitic.common.tasks.messages.ForMonitoringPerNameMessage
import io.infinitic.common.tasks.messages.ForTaskEngineMessage
import io.infinitic.common.tasks.messages.ForWorkerMessage
import io.infinitic.common.workflows.messages.ForWorkflowEngineMessage
import io.infinitic.engine.taskManager.engines.MonitoringGlobal
import io.infinitic.engine.taskManager.engines.MonitoringPerName
import io.infinitic.engine.taskManager.engines.TaskEngine
import io.infinitic.tests.tasks.inMemory.InMemoryEmetter
import io.infinitic.tests.tasks.inMemory.InMemoryReceiver
import io.infinitic.tests.tasks.inMemory.InMemoryReceiverTest
import io.infinitic.tests.tasks.inMemory.InMemoryStorageTest
import io.infinitic.tests.tasks.samples.Status
import io.infinitic.tests.tasks.samples.TaskTest
import io.infinitic.tests.tasks.samples.TaskTestImpl
import io.infinitic.worker.Worker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.runBlocking

private val storage = InMemoryStorageTest()
private val forWorkflowEngineTransport = Channel<ForWorkflowEngineMessage>(64)
private val forTaskEngineTransport = Channel<ForTaskEngineMessage>(64)
private val forMonitoringPerNameTransport = Channel<ForMonitoringPerNameMessage>(64)
private val forMonitoringGlobalTransport = Channel<ForMonitoringGlobalMessage>(64)
private val forWorkerTransport = Channel<ForWorkerMessage>(64)
private val monitoringPerNameReceiver = InMemoryReceiverTest(forMonitoringPerNameTransport)

private val client = Client(
    InMemoryEmetter(forTaskEngineTransport),
    InMemoryEmetter(forWorkflowEngineTransport)
)
private val taskEngine = TaskEngine(
    storage,
    InMemoryReceiverTest(forTaskEngineTransport),
    InMemoryEmetter(forTaskEngineTransport),
    InMemoryEmetter(forMonitoringPerNameTransport),
    InMemoryEmetter(forWorkerTransport)
)
private val monitoringPerNameEngine = MonitoringPerName(
    storage,
    monitoringPerNameReceiver,
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

@ExperimentalCoroutinesApi
fun main()  {
    runBlocking {
        val taskTest = TaskTestImpl()
        taskTest.behavior = { _, _ -> Status.SUCCESS }
        worker.register<TaskTest>() { taskTest }

        startListen(this)

        val task = client.dispatch(TaskTest::class.java) { log() }

        checkActive(this)

        println(monitoringPerNameReceiver.taskStatus)
    }
}

suspend fun startListen(scope: CoroutineScope) {
    taskEngine.listen(scope)
    monitoringPerNameEngine.listen(scope)
    monitoringGlobalEngine.listen(scope)
    worker.listen(scope)
}

suspend fun checkActive(scope: CoroutineScope) {
    while (scope.isActive) {
        delay(1000)
        if (
            forWorkflowEngineTransport.isEmpty &&
            forTaskEngineTransport.isEmpty &&
            forMonitoringPerNameTransport.isEmpty &&
            forMonitoringGlobalTransport.isEmpty &&
            forWorkerTransport.isEmpty
        ) scope.cancel()
    }
}
