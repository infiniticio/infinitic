package io.infinitic.messaging.api.dispatcher

import io.infinitic.common.avro.AvroSerDe
import io.infinitic.messaging.api.dispatcher.transport.BinaryCompatibleTransport
import io.infinitic.common.tasks.avro.AvroConverter as TaskAvroConverter
import io.infinitic.common.tasks.messages.ForMonitoringGlobalMessage
import io.infinitic.common.tasks.messages.ForMonitoringPerNameMessage
import io.infinitic.common.tasks.messages.ForTaskEngineMessage
import io.infinitic.common.tasks.messages.ForWorkerMessage
import io.infinitic.common.workflows.avro.AvroConverter as WorkflowAvroConverter
import io.infinitic.common.workflows.messages.ForWorkflowEngineMessage

class BinaryDispatcher(private val transport: BinaryCompatibleTransport) : Dispatcher {
    override suspend fun toWorkflowEngine(msg: ForWorkflowEngineMessage, after: Float) {
        msg
            .let { WorkflowAvroConverter.toWorkflowEngine(msg) }
            .let { AvroSerDe.serialize(it) }
            .let { transport.toWorkflowEngine(it, after) }
    }

    override suspend fun toTaskEngine(msg: ForTaskEngineMessage, after: Float) {
        msg
            .let { TaskAvroConverter.toTaskEngine(msg) }
            .let { AvroSerDe.serialize(it) }
            .let { transport.toTaskEngine(it, after) }
    }

    override suspend fun toMonitoringPerName(msg: ForMonitoringPerNameMessage) {
        msg
            .let { TaskAvroConverter.toMonitoringPerName(it) }
            .let { AvroSerDe.serialize(it) }
            .let { transport.toMonitoringPerName(it) }
    }

    override suspend fun toMonitoringGlobal(msg: ForMonitoringGlobalMessage) {
        msg
            .let { TaskAvroConverter.toMonitoringGlobal(it) }
            .let { AvroSerDe.serialize(it) }
            .let { transport.toMonitoringGlobal(it) }
    }

    override suspend fun toWorkers(msg: ForWorkerMessage) {
        msg
            .let { TaskAvroConverter.toWorkers(it) }
            .let { AvroSerDe.serialize(it) }
            .let { transport.toWorkers(it) }
    }
}
