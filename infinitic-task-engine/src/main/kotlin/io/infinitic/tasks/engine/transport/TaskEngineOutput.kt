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

package io.infinitic.tasks.engine.transport

import io.infinitic.common.monitoring.perName.messages.MonitoringPerNameEngineMessage
import io.infinitic.common.monitoring.perName.transport.SendToMonitoringPerName
import io.infinitic.common.tasks.data.TaskId
import io.infinitic.common.tasks.engine.messages.TaskEngineMessage
import io.infinitic.common.tasks.engine.transport.SendToTaskEngine
import io.infinitic.common.tasks.executors.SendToTaskExecutors
import io.infinitic.common.tasks.executors.messages.TaskExecutorMessage
import io.infinitic.common.workflows.engine.messages.WorkflowEngineMessage
import io.infinitic.common.workflows.engine.transport.SendToWorkflowEngine
import org.slf4j.Logger
import org.slf4j.LoggerFactory

interface TaskEngineOutput {
    val sendToWorkflowEngineFn: SendToWorkflowEngine
    val sendToTaskEngineFn: SendToTaskEngine
    val sendToTaskExecutorsFn: SendToTaskExecutors
    val sendToMonitoringPerNameFn: SendToMonitoringPerName

    private val logger: Logger
        get() = LoggerFactory.getLogger(javaClass)

    suspend fun sendToWorkflowEngine(
        taskId: TaskId,
        workflowEngineMessage: WorkflowEngineMessage,
        after: Float
    ) {
        sendToWorkflowEngineFn(workflowEngineMessage, after)
        logger.debug("taskId {} - after {} sendToWorkflowEngine {}", taskId, after, workflowEngineMessage)
    }

    suspend fun sendToTaskEngine(
        taskId: TaskId,
        taskEngineMessage: TaskEngineMessage,
        after: Float
    ) {
        sendToTaskEngineFn(taskEngineMessage, after)
        logger.debug("taskId {} - after {} sendToTaskEngine {}", taskId, after, taskEngineMessage)
    }

    suspend fun sendToTaskExecutors(
        taskId: TaskId,
        taskExecutorMessage: TaskExecutorMessage
    ) {
        sendToTaskExecutorsFn(taskExecutorMessage)
        logger.debug("taskId {} - after {} sendToTaskEngine {}", taskId, taskExecutorMessage)
    }

    suspend fun sendToMonitoringPerName(
        taskId: TaskId,
        monitoringPerNameEngineMessage: MonitoringPerNameEngineMessage
    ) {
        sendToMonitoringPerNameFn(monitoringPerNameEngineMessage)
        logger.debug("taskId {} - after {} sendToMonitoringPerName {}", taskId, monitoringPerNameEngineMessage)
    }
}
