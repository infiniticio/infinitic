package io.infinitic.taskManager.engine.dispatcher

import io.infinitic.taskManager.common.messages.ForMonitoringGlobalMessage
import io.infinitic.taskManager.common.messages.ForMonitoringPerNameMessage
import io.infinitic.taskManager.common.messages.ForTaskEngineMessage
import io.infinitic.taskManager.common.messages.ForWorkerMessage

interface EngineDispatcher {
    suspend fun toTaskEngine(msg: ForTaskEngineMessage, after: Float = 0f)
    suspend fun toMonitoringPerName(msg: ForMonitoringPerNameMessage)
    suspend fun toMonitoringGlobal(msg: ForMonitoringGlobalMessage)
    suspend fun toWorkers(msg: ForWorkerMessage)
}
