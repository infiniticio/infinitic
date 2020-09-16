package io.infinitic.messaging.api.dispatcher

import io.infinitic.taskManager.common.messages.ForMonitoringGlobalMessage
import io.infinitic.taskManager.common.messages.ForMonitoringPerNameMessage
import io.infinitic.taskManager.common.messages.ForTaskEngineMessage
import io.infinitic.taskManager.common.messages.ForWorkerMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class InMemoryDispatcher() : Dispatcher {
    // Here we favor lambda to avoid a direct dependency with engines instances
    lateinit var taskEngineHandle: suspend (msg: ForTaskEngineMessage) -> Unit
    lateinit var monitoringPerNameHandle: suspend (msg: ForMonitoringPerNameMessage) -> Unit
    lateinit var monitoringGlobalHandle: suspend (msg: ForMonitoringGlobalMessage) -> Unit
    lateinit var workerHandle: suspend (msg: ForWorkerMessage) -> Unit
    lateinit var scope: CoroutineScope

    override suspend fun toTaskEngine(msg: ForTaskEngineMessage, after: Float) {
        scope.launch {
            if (after > 0F) {
                delay((1000 * after).toLong())
            }
            taskEngineHandle(msg)
        }
    }

    override suspend fun toMonitoringPerName(msg: ForMonitoringPerNameMessage) {
        scope.launch { monitoringPerNameHandle(msg) }
    }

    override suspend fun toMonitoringGlobal(msg: ForMonitoringGlobalMessage) {
        scope.launch { monitoringGlobalHandle(msg) }
    }

    override suspend fun toWorkers(msg: ForWorkerMessage) {
        scope.launch { workerHandle(msg) }
    }
}
