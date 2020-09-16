package io.infinitic.messaging.api.dispatcher

import io.infinitic.messaging.api.dispatcher.transport.AvroCompatibleTransport
import io.infinitic.taskManager.common.avro.AvroConverter
import io.infinitic.taskManager.common.messages.ForMonitoringGlobalMessage
import io.infinitic.taskManager.common.messages.ForMonitoringPerNameMessage
import io.infinitic.taskManager.common.messages.ForTaskEngineMessage
import io.infinitic.taskManager.common.messages.ForWorkerMessage

class AvroDispatcher(private val transport: AvroCompatibleTransport) : Dispatcher {
    override suspend fun toTaskEngine(msg: ForTaskEngineMessage, after: Float) {
        msg
            .let { AvroConverter.toTaskEngine(it) }
            .let { transport.toTaskEngine(it, after) }
    }

    override suspend fun toMonitoringPerName(msg: ForMonitoringPerNameMessage) {
        msg
            .let { AvroConverter.toMonitoringPerName(it) }
            .let { transport.toMonitoringPerName(it) }
    }

    override suspend fun toMonitoringGlobal(msg: ForMonitoringGlobalMessage) {
        msg
            .let { AvroConverter.toMonitoringGlobal(it) }
            .let { transport.toMonitoringGlobal(it) }
    }

    override suspend fun toWorkers(msg: ForWorkerMessage) {
        msg
            .let { AvroConverter.toWorkers(it) }
            .let { transport.toWorkers(it) }
    }
}
