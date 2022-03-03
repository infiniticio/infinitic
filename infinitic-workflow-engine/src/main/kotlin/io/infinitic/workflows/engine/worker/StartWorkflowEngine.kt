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

package io.infinitic.workflows.engine.worker

import io.infinitic.common.clients.transport.SendToClient
import io.infinitic.common.data.ClientName
import io.infinitic.common.tasks.engine.SendToTaskEngine
import io.infinitic.common.tasks.tags.SendToTaskTag
import io.infinitic.common.workers.MessageToProcess
import io.infinitic.common.workflows.engine.SendToWorkflowEngine
import io.infinitic.common.workflows.engine.SendToWorkflowEngineAfter
import io.infinitic.common.workflows.engine.messages.WorkflowEngineMessage
import io.infinitic.common.workflows.engine.storage.WorkflowStateStorage
import io.infinitic.common.workflows.tags.SendToWorkflowTag
import io.infinitic.workflows.engine.WorkflowEngine
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.launch
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

typealias WorkflowEngineMessageToProcess = MessageToProcess<WorkflowEngineMessage>

private fun logError(messageToProcess: WorkflowEngineMessageToProcess, e: Throwable) = logger.error(e) {
    "exception on message ${messageToProcess.message}: $e"
}

fun <T : WorkflowEngineMessageToProcess> CoroutineScope.startWorkflowEngine(
    name: String,
    workflowStateStorage: WorkflowStateStorage,
    inputChannel: ReceiveChannel<T>,
    outputChannel: SendChannel<T>,
    sendEventsToClient: SendToClient,
    sendToTaskTagEngine: SendToTaskTag,
    sendToTaskEngine: SendToTaskEngine,
    sendToWorkflowTaskEngine: SendToTaskEngine,
    sendToWorkflowTagEngine: SendToWorkflowTag,
    sendToWorkflowEngine: SendToWorkflowEngine,
    sendToWorkflowEngineAfter: SendToWorkflowEngineAfter
) = launch(CoroutineName(name)) {

    val workflowEngine = WorkflowEngine(
        ClientName(name),
        workflowStateStorage,
        sendEventsToClient,
        sendToTaskTagEngine,
        sendToTaskEngine,
        sendToWorkflowTaskEngine,
        sendToWorkflowTagEngine,
        sendToWorkflowEngine,
        sendToWorkflowEngineAfter
    )

    for (message in inputChannel) {
        try {
            message.returnValue = workflowEngine.handle(message.message)
        } catch (e: Throwable) {
            message.throwable = e
            logError(message, e)
        } finally {
            outputChannel.send(message)
        }
    }
}
