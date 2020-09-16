package io.infinitic.messaging.api.dispatcher

import io.infinitic.common.avro.AvroSerDe
import io.infinitic.messaging.api.dispatcher.transport.BinaryCompatibleTransport
import io.infinitic.taskManager.common.avro.AvroConverter
import io.infinitic.taskManager.common.messages.ForMonitoringGlobalMessage
import io.infinitic.taskManager.common.messages.ForMonitoringPerNameMessage
import io.infinitic.taskManager.common.messages.ForTaskEngineMessage
import io.infinitic.taskManager.common.messages.ForWorkerMessage

class BinaryDispatcher(private val transport: BinaryCompatibleTransport) : Dispatcher {
    override suspend fun toTaskEngine(msg: ForTaskEngineMessage, after: Float) {
        msg
            .let { AvroConverter.toTaskEngine(msg) }
            .let { AvroSerDe.serialize(it) }
            .let { transport.toTaskEngine(it, after) }
    }

    override suspend fun toMonitoringPerName(msg: ForMonitoringPerNameMessage) {
        msg
            .let { AvroConverter.toMonitoringPerName(it) }
            .let { AvroSerDe.serialize(it) }
            .let { transport.toMonitoringPerName(it) }
    }

    override suspend fun toMonitoringGlobal(msg: ForMonitoringGlobalMessage) {
        msg
            .let { AvroConverter.toMonitoringGlobal(it) }
            .let { AvroSerDe.serialize(it) }
            .let { transport.toMonitoringGlobal(it) }
    }

    override suspend fun toWorkers(msg: ForWorkerMessage) {
        msg
            .let { AvroConverter.toWorkers(it) }
            .let { AvroSerDe.serialize(it) }
            .let { transport.toWorkers(it) }
    }
}
