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

package io.infinitic.tags.engine.transport

import io.infinitic.common.clients.messages.ClientResponseMessage
import io.infinitic.common.clients.transport.SendToClientResponse
import io.infinitic.common.data.MessageId
import io.infinitic.common.data.MillisDuration
import io.infinitic.common.tags.data.Tag
import io.infinitic.common.tasks.engine.messages.TaskEngineMessage
import io.infinitic.common.tasks.engine.transport.SendToTaskEngine
import io.infinitic.common.workflows.engine.messages.WorkflowEngineMessage
import io.infinitic.common.workflows.engine.transport.SendToWorkflowEngine
import org.slf4j.Logger
import org.slf4j.LoggerFactory

interface TagEngineOutput {
    val sendToClientResponseFn: SendToClientResponse
    val sendToWorkflowEngineFn: SendToWorkflowEngine
    val sendToTaskEngineFn: SendToTaskEngine

    private val logger: Logger
        get() = LoggerFactory.getLogger(javaClass)

    suspend fun sendToClientResponse(
        messageId: MessageId,
        tag: Tag,
        clientResponseMessage: ClientResponseMessage
    ) {
        logger.debug(
            "from messageId {}: tag {} - sendToClientResponse {}",
            messageId,
            tag,
            clientResponseMessage
        )
        sendToClientResponseFn(clientResponseMessage)
    }

    suspend fun sendToTaskEngine(
        messageId: MessageId,
        tag: Tag,
        taskEngineMessage: TaskEngineMessage
    ) {
        logger.debug(
            "from messageId {}: tag {} - sendToTaskEngine {}",
            messageId,
            tag,
            taskEngineMessage
        )
        sendToTaskEngineFn(taskEngineMessage, MillisDuration(0))
    }

    suspend fun sendToWorkflowEngine(
        messageId: MessageId,
        tag: Tag,
        workflowEngineMessage: WorkflowEngineMessage
    ) {
        logger.debug(
            "from messageId {}: tag {} - sendToWorkflowEngine {}",
            messageId,
            tag,
            workflowEngineMessage
        )
        sendToWorkflowEngineFn(workflowEngineMessage, MillisDuration(0))
    }
}
