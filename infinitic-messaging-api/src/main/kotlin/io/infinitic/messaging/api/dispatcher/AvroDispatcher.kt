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

import io.infinitic.avro.taskManager.messages.AvroDispatchTask
import io.infinitic.common.json.Json
import io.infinitic.common.tasks.messages.DispatchTask
import io.infinitic.messaging.api.dispatcher.transport.AvroTransport
import io.infinitic.common.tasks.avro.AvroConverter as TaskAvroConverter
import io.infinitic.common.tasks.messages.ForMonitoringGlobalMessage
import io.infinitic.common.tasks.messages.ForMonitoringPerNameMessage
import io.infinitic.common.tasks.messages.ForTaskEngineMessage
import io.infinitic.common.tasks.messages.ForWorkerMessage
import io.infinitic.common.workflows.avro.AvroConverter as WorkflowAvroConverter
import io.infinitic.common.workflows.messages.ForWorkflowEngineMessage

class AvroDispatcher(private val transport: AvroTransport) : Dispatcher {
    override suspend fun toWorkflowEngine(msg: ForWorkflowEngineMessage, after: Float) {
        msg
            .let { WorkflowAvroConverter.toWorkflowEngine(it) }
            .let { transport.toWorkflowEngine(it, after) }
    }

    override suspend fun toTaskEngine(msg: ForTaskEngineMessage, after: Float) {
        msg
            .let { TaskAvroConverter.toTaskEngine(it) }
            .let { transport.toTaskEngine(it, after) }
    }

    override suspend fun toMonitoringPerName(msg: ForMonitoringPerNameMessage) {
        msg
            .let { TaskAvroConverter.toMonitoringPerName(it) }
            .let { transport.toMonitoringPerName(it) }
    }

    override suspend fun toMonitoringGlobal(msg: ForMonitoringGlobalMessage) {
        msg
            .let { TaskAvroConverter.toMonitoringGlobal(it) }
            .let { transport.toMonitoringGlobal(it) }
    }

    override suspend fun toWorkers(msg: ForWorkerMessage) {
        msg
            .let { TaskAvroConverter.toWorkers(it) }
            .let { transport.toWorkers(it) }
    }
}
