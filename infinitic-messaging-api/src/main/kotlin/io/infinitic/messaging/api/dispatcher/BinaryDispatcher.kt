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

package io.infinitic.messaging.api.dispatcher

import io.infinitic.common.avro.AvroSerDe
import io.infinitic.messaging.api.dispatcher.transport.BinaryTransport
import io.infinitic.common.serDe.avro.AvroConverter as TaskAvroConverter
import io.infinitic.common.tasks.messages.monitoringGlobalMessages.MonitoringGlobalMessage
import io.infinitic.common.tasks.messages.monitoringPerNameMessages.MonitoringPerNameMessage
import io.infinitic.common.tasks.messages.taskEngineMessages.TaskEngineMessage
import io.infinitic.common.tasks.messages.workerMessages.WorkerMessage
import io.infinitic.common.workflows.avro.AvroConverter as WorkflowAvroConverter
import io.infinitic.common.workflows.messages.WorkflowEngineMessage

class BinaryDispatcher(private val transport: BinaryTransport) : Dispatcher {
    override suspend fun toWorkflowEngine(msg: WorkflowEngineMessage, after: Float) {
        msg
            .let { WorkflowAvroConverter.toWorkflowEngine(it) }
            .let { AvroSerDe.serialize(it) }
            .let { transport.toWorkflowEngine(it, after) }
    }

    override suspend fun toTaskEngine(msg: TaskEngineMessage, after: Float) {
        msg
            .let { TaskAvroConverter.toTaskEngine(it) }
            .let { AvroSerDe.serialize(it) }
            .let { transport.toTaskEngine(it, after) }
    }

    override suspend fun toMonitoringPerName(msg: MonitoringPerNameMessage) {
        msg
            .let { TaskAvroConverter.toMonitoringPerName(it) }
            .let { AvroSerDe.serialize(it) }
            .let { transport.toMonitoringPerName(it) }
    }

    override suspend fun toMonitoringGlobal(msg: MonitoringGlobalMessage) {
        msg
            .let { TaskAvroConverter.toMonitoringGlobal(it) }
            .let { AvroSerDe.serialize(it) }
            .let { transport.toMonitoringGlobal(it) }
    }

    override suspend fun toWorkers(msg: WorkerMessage) {
        msg
            .let { TaskAvroConverter.toWorkers(it) }
            .let { AvroSerDe.serialize(it) }
            .let { transport.toWorkers(it) }
    }
}
