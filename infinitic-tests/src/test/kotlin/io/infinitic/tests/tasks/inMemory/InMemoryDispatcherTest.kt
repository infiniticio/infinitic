package io.infinitic.tests.tasks.inMemory

import io.infinitic.messaging.api.dispatcher.InMemoryDispatcher
import io.infinitic.client.Client
import io.infinitic.common.taskManager.data.TaskStatus
import io.infinitic.common.taskManager.messages.TaskStatusUpdated
import io.infinitic.engine.taskManager.engines.MonitoringGlobal
import io.infinitic.engine.taskManager.engines.MonitoringPerName
import io.infinitic.engine.taskManager.engines.TaskEngine
import io.infinitic.engine.taskManager.storage.InMemoryTaskStateStorage
import io.infinitic.worker.Worker

class InMemoryDispatcherTest(storage: InMemoryTaskStateStorage) : InMemoryDispatcher() {
    val client = Client(this)
    val worker = Worker(this)
    val taskEngine = TaskEngine(storage, this)
    val monitoringPerName = MonitoringPerName(storage, this)
    val monitoringGlobal = MonitoringGlobal(storage)

    lateinit var taskStatus: TaskStatus

    init {
        taskEngineHandle = { taskEngine.handle(it) }
        monitoringPerNameHandle = {
            monitoringPerName.handle(it)
            when (it) {
                is TaskStatusUpdated -> { taskStatus = it.newStatus }
            }
        }
        monitoringGlobalHandle = { monitoringGlobal.handle(it) }
        workerHandle = { worker.handle(it) }
    }
}
