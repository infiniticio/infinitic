package io.infinitic.messaging.api.dispatcher

import io.infinitic.common.tasks.messages.ForMonitoringGlobalMessage
import io.infinitic.common.tasks.messages.ForMonitoringPerNameMessage
import io.infinitic.common.tasks.messages.ForTaskEngineMessage
import io.infinitic.common.tasks.messages.ForWorkerMessage
import io.infinitic.common.workflows.messages.ForWorkflowEngineMessage

interface Dispatcher {
    suspend fun toWorkflowEngine(msg: ForWorkflowEngineMessage, after: Float = 0f)
    suspend fun toTaskEngine(msg: ForTaskEngineMessage, after: Float = 0f)
    suspend fun toMonitoringPerName(msg: ForMonitoringPerNameMessage)
    suspend fun toMonitoringGlobal(msg: ForMonitoringGlobalMessage)
    suspend fun toWorkers(msg: ForWorkerMessage)
}
