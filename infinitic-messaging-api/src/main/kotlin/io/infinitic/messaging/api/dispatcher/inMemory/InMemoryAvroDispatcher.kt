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

package io.infinitic.messaging.api.dispatcher.inMemory

import io.infinitic.common.tasks.messages.ForMonitoringGlobalMessage
import io.infinitic.common.tasks.messages.ForMonitoringPerNameMessage
import io.infinitic.common.tasks.messages.ForTaskEngineMessage
import io.infinitic.common.tasks.messages.ForWorkerMessage
import io.infinitic.common.workflows.avro.AvroConverter
import io.infinitic.common.workflows.messages.ForWorkflowEngineMessage
import io.infinitic.messaging.api.dispatcher.Dispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import io.infinitic.common.tasks.avro.AvroConverter as TaskAvroConverter

open class InMemoryAvroDispatcher : Dispatcher {
    // Here we favor lambda to avoid a direct dependency with engines instances
    lateinit var workflowEngineHandle: suspend (msg: ForWorkflowEngineMessage) -> Unit
    lateinit var taskEngineHandle: suspend (msg: ForTaskEngineMessage) -> Unit
    lateinit var monitoringPerNameHandle: suspend (msg: ForMonitoringPerNameMessage) -> Unit
    lateinit var monitoringGlobalHandle: suspend (msg: ForMonitoringGlobalMessage) -> Unit
    lateinit var workerHandle: suspend (msg: ForWorkerMessage) -> Unit
    lateinit var scope: CoroutineScope

    override suspend fun toWorkflowEngine(msg: ForWorkflowEngineMessage, after: Float) {
        val avro = AvroConverter.toWorkflowEngine(msg)

        scope.launch {
            if (after > 0F) {
                delay((1000 * after).toLong())
            }
            workflowEngineHandle(AvroConverter.fromWorkflowEngine(avro))
        }
    }

    override suspend fun toTaskEngine(msg: ForTaskEngineMessage, after: Float) {
        val avro = TaskAvroConverter.toTaskEngine(msg)

        scope.launch {
            if (after > 0F) {
                delay((1000 * after).toLong())
            }
            taskEngineHandle(TaskAvroConverter.fromTaskEngine(avro))
        }
    }

    override suspend fun toMonitoringPerName(msg: ForMonitoringPerNameMessage) {
        scope.launch {
            val avro = TaskAvroConverter.toMonitoringPerName(msg)

            monitoringPerNameHandle(TaskAvroConverter.fromMonitoringPerName(avro)) }
    }

    override suspend fun toMonitoringGlobal(msg: ForMonitoringGlobalMessage) {
        val avro = TaskAvroConverter.toMonitoringGlobal(msg)

        scope.launch { monitoringGlobalHandle(TaskAvroConverter.fromMonitoringGlobal(avro)) }
    }

    override suspend fun toWorkers(msg: ForWorkerMessage) {
        val avro = TaskAvroConverter.toWorkers(msg)

        scope.launch { workerHandle(TaskAvroConverter.fromWorkers(avro)) }
    }


}
