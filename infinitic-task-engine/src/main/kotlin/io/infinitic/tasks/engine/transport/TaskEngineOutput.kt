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

import io.infinitic.common.clients.messages.ClientResponseMessage
import io.infinitic.common.clients.transport.SendToClientResponse
import io.infinitic.common.data.MillisDuration
import io.infinitic.common.monitoring.perName.messages.MonitoringPerNameMessage
import io.infinitic.common.monitoring.perName.transport.SendToMonitoringPerName
import io.infinitic.common.tags.messages.TagEngineMessage
import io.infinitic.common.tags.transport.SendToTagEngine
import io.infinitic.common.tasks.engine.messages.TaskEngineMessage
import io.infinitic.common.tasks.engine.state.TaskState
import io.infinitic.common.tasks.engine.transport.SendToTaskEngine
import io.infinitic.common.tasks.executors.SendToTaskExecutors
import io.infinitic.common.tasks.executors.messages.TaskExecutorMessage
import io.infinitic.common.workflows.engine.messages.WorkflowEngineMessage
import io.infinitic.common.workflows.engine.transport.SendToWorkflowEngine
import org.slf4j.Logger
import org.slf4j.LoggerFactory

interface TaskEngineOutput {
    val sendToClientResponseFn: SendToClientResponse
    val sendToTagEngineFn: SendToTagEngine
    val sendToTaskEngineFn: SendToTaskEngine
    val sendToWorkflowEngineFn: SendToWorkflowEngine
    val sendToTaskExecutorsFn: SendToTaskExecutors
    val sendToMonitoringPerNameFn: SendToMonitoringPerName

    private val logger: Logger
        get() = LoggerFactory.getLogger(javaClass)

    suspend fun sendToClientResponse(
        state: TaskState,
        clientResponseMessage: ClientResponseMessage
    ) {
        logger.debug(
            "from messageId {}: taskId {} - sendToClientResponse {}",
            state.lastMessageId,
            state.taskId,
            clientResponseMessage
        )
        sendToClientResponseFn(clientResponseMessage)
    }

    suspend fun sendToTagEngine(
        state: TaskState,
        tagEngineMessage: TagEngineMessage
    ) {
        logger.debug(
            "from messageId {}: taskId {} - sendToTagEngine {}",
            state.lastMessageId,
            state.taskId,
            tagEngineMessage
        )
        sendToTagEngineFn(tagEngineMessage)
    }

    suspend fun sendToTaskEngine(
        state: TaskState,
        taskEngineMessage: TaskEngineMessage,
        after: MillisDuration
    ) {
        logger.debug(
            "from messageId {}: taskId {} - after {} sendToTaskEngine {}",
            state.lastMessageId,
            state.taskId,
            after,
            taskEngineMessage
        )
        sendToTaskEngineFn(taskEngineMessage, after)
    }

    suspend fun sendToWorkflowEngine(
        state: TaskState,
        workflowEngineMessage: WorkflowEngineMessage,
        after: MillisDuration
    ) {
        logger.debug(
            "from messageId {}: taskId {} - after {} sendToWorkflowEngine {}",
            state.lastMessageId,
            state.taskId,
            after,
            workflowEngineMessage
        )
        sendToWorkflowEngineFn(workflowEngineMessage, after)
    }

    suspend fun sendToTaskExecutors(
        state: TaskState,
        taskExecutorMessage: TaskExecutorMessage
    ) {
        logger.debug(
            "from messageId {}: taskId {} - sendToTaskExecutors {}",
            state.lastMessageId,
            state.taskId,
            taskExecutorMessage
        )
        sendToTaskExecutorsFn(taskExecutorMessage)
    }

    suspend fun sendToMonitoringPerName(
        state: TaskState,
        monitoringPerNameMessage: MonitoringPerNameMessage
    ) {
        logger.debug(
            "from messageId {}: taskId {} - sendToMonitoringPerName {}",
            state.lastMessageId,
            state.taskId,
            monitoringPerNameMessage
        )
        sendToMonitoringPerNameFn(monitoringPerNameMessage)
    }
}
