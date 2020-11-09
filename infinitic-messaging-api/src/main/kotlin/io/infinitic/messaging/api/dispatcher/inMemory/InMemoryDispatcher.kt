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

import io.infinitic.common.tasks.messages.monitoringGlobalMessages.MonitoringGlobalMessage
import io.infinitic.common.tasks.messages.monitoringPerNameMessages.MonitoringPerNameEngineMessage
import io.infinitic.common.tasks.messages.taskEngineMessages.TaskEngineMessage
import io.infinitic.common.tasks.messages.workerMessages.WorkerMessage
import io.infinitic.common.workflows.messages.WorkflowEngineMessage
import io.infinitic.messaging.api.dispatcher.Dispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

open class InMemoryDispatcher() : Dispatcher {
    // Here we favor lambda to avoid a direct dependency with engines instances
    lateinit var workflowEngineHandle: suspend (msg: WorkflowEngineMessage) -> Unit
    lateinit var taskEngineHandle: suspend (msg: TaskEngineMessage) -> Unit
    lateinit var monitoringPerNameHandle: suspend (msg: MonitoringPerNameEngineMessage) -> Unit
    lateinit var monitoringGlobalHandle: suspend (msg: MonitoringGlobalMessage) -> Unit
    lateinit var workerHandle: suspend (msg: WorkerMessage) -> Unit
    lateinit var scope: CoroutineScope

    override suspend fun toWorkflowEngine(msg: WorkflowEngineMessage, after: Float) {
        scope.launch {
            if (after > 0F) {
                delay((1000 * after).toLong())
            }
            workflowEngineHandle(msg)
        }
    }

    override suspend fun toTaskEngine(msg: TaskEngineMessage, after: Float) {
        scope.launch {
            if (after > 0F) {
                delay((1000 * after).toLong())
            }
            taskEngineHandle(msg)
        }
    }

    override suspend fun toMonitoringPerNameEngine(msg: MonitoringPerNameEngineMessage) {
        scope.launch {
            monitoringPerNameHandle(msg)
        }
    }

    override suspend fun toMonitoringGlobalEngine(msg: MonitoringGlobalMessage) {
        scope.launch {
            monitoringGlobalHandle(msg)
        }
    }

    override suspend fun toWorkers(msg: WorkerMessage) {
        scope.launch {
            workerHandle(msg)
        }
    }
}
