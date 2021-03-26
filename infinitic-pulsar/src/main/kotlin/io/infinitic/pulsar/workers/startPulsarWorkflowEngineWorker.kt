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

package io.infinitic.pulsar.workers

import io.infinitic.common.storage.keyValue.KeyValueCache
import io.infinitic.common.storage.keyValue.KeyValueStorage
import io.infinitic.common.workers.singleThreadedContext
import io.infinitic.common.workflows.engine.messages.WorkflowEngineEnvelope
import io.infinitic.common.workflows.engine.messages.WorkflowEngineMessage
import io.infinitic.common.workflows.engine.state.WorkflowState
import io.infinitic.common.workflows.engine.transport.SendToWorkflowEngine
import io.infinitic.pulsar.InfiniticWorker
import io.infinitic.pulsar.transport.PulsarMessageToProcess
import io.infinitic.workflows.engine.WorkflowEngine
import io.infinitic.workflows.engine.storage.events.NoWorkflowEventStorage
import io.infinitic.workflows.engine.storage.states.WorkflowStateCachedKeyStorage
import io.infinitic.workflows.engine.transport.WorkflowEngineOutput
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.future.await
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.apache.pulsar.client.api.Consumer
import org.apache.pulsar.client.api.Message
import org.apache.pulsar.client.api.MessageId
import org.slf4j.Logger
import org.slf4j.LoggerFactory

typealias PulsarWorkflowEngineMessageToProcess = PulsarMessageToProcess<WorkflowEngineMessage>

const val WORKFLOW_ENGINE_THREAD_NAME = "workflow-engine"

private val logger: Logger
    get() = LoggerFactory.getLogger(InfiniticWorker::class.java)

private fun logError(message: Message<WorkflowEngineEnvelope>, e: Exception) = logger.error(
    "exception on Pulsar Message {}:${System.getProperty("line.separator")}{}",
    message,
    e
)

private fun logError(message: WorkflowEngineMessage, e: Exception) = logger.error(
    "workflowId {} - exception on message {}:${System.getProperty("line.separator")}{}",
    message.workflowId,
    message,
    e
)

fun CoroutineScope.startPulsarWorkflowEngineWorker(
    consumerCounter: Int,
    workflowEngineConsumer: Consumer<WorkflowEngineEnvelope>,
    workflowEngineOutput: WorkflowEngineOutput,
    sendToWorkflowEngineDeadLetters: SendToWorkflowEngine,
    keyValueStorage: KeyValueStorage,
    keyValueCache: KeyValueCache<WorkflowState>
) = launch(singleThreadedContext("$WORKFLOW_ENGINE_THREAD_NAME-$consumerCounter")) {

    val workflowEngine = WorkflowEngine(
        WorkflowStateCachedKeyStorage(keyValueStorage, keyValueCache),
        NoWorkflowEventStorage(),
        workflowEngineOutput
    )

    fun negativeAcknowledge(pulsarId: MessageId) =
        workflowEngineConsumer.negativeAcknowledge(pulsarId)

    suspend fun acknowledge(pulsarId: MessageId) =
        workflowEngineConsumer.acknowledgeAsync(pulsarId).await()

    while (isActive) {
        val pulsarMessage = workflowEngineConsumer.receiveAsync().await()

        val message = try {
            WorkflowEngineEnvelope.fromByteArray(pulsarMessage.data).message()
        } catch (e: Exception) {
            logError(pulsarMessage, e)
            negativeAcknowledge(pulsarMessage.messageId)

            null
        }

        message?.let {
            try {
                workflowEngine.handle(it)

                acknowledge(pulsarMessage.messageId)
            } catch (e: Exception) {
                logError(message, e)
                negativeAcknowledge(pulsarMessage.messageId)
            }
        }
    }
}
