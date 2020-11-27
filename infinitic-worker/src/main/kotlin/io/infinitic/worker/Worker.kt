/**
 * "Commons Clause" License Condition v1.0
 *
 * The Software is provided to you by the Licensor under the License, as defined
 * below, subject to the following condition.
 *
 * Without limiting other conditions in the License, the grant of rights under the
 * License will not include, and the License does not grant to you, the right to
 * Sell the Software.
 *
 * For purposes of the foregoing, “Sell” means practicing any or all of the rights
 * granted to you under the License to provide to third parties, for a fee or
 * other consideration (including without limitation fees for hosting or
 * consulting/ support services related to the Software), a product or service
 * whose value derives, entirely or substantially, from the functionality of the
 * Software. Any license notice or attribution required by the License must also
 * include this Commons Clause License Condition notice.
 *
 * Software: Infinitic
 *
 * License: MIT License (https://opensource.org/licenses/MIT)
 *
 * Licensor: infinitic.io
 */

package io.infinitic.worker

import io.infinitic.common.SendToMonitoringGlobal
import io.infinitic.common.SendToMonitoringPerName
import io.infinitic.common.SendToTaskEngine
import io.infinitic.common.SendToWorkers
import io.infinitic.common.SendToWorkflowEngine
import io.infinitic.monitoring.global.engine.storage.MonitoringGlobalStateStorage
import io.infinitic.monitoring.perName.engine.storage.MonitoringPerNameStateStorage
import io.infinitic.tasks.engine.storage.TaskStateStorage
import io.infinitic.worker.taskEngineWorker.TaskEngineWorker
import io.infinitic.worker.taskExecutorWorker.TaskExecutorWorker
import io.infinitic.worker.workflowEngineWorker.WorkflowEngineWorker
import io.infinitic.worker.workflowExecutorWorker.WorkflowExecutorWorker
import io.infinitic.workflows.engine.storage.WorkflowStateStorage

class Worker(
    sendToWorkflowEngine: SendToWorkflowEngine,
    sendToTaskEngine: SendToTaskEngine,
    sendToMonitoringPerName: SendToMonitoringPerName,
    sendToMonitoringGlobal: SendToMonitoringGlobal,
    sendToWorkers: SendToWorkers,
    workflowStateStorage: WorkflowStateStorage,
    taskStateStorage: TaskStateStorage,
    monitoringPerNameStateStorage: MonitoringPerNameStateStorage,
    monitoringGlobalStateStorage: MonitoringGlobalStateStorage,
) {
    val taskEngineWorker: TaskEngineWorker? = null
    val taskExecutorWorker: TaskExecutorWorker? = null
    val workflowEngineWorker: WorkflowEngineWorker? = null
    val workflowExecutorWorker: WorkflowExecutorWorker? = null

    companion object {
    }
}
