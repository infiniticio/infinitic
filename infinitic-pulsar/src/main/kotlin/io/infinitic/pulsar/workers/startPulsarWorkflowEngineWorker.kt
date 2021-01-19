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

import io.infinitic.common.storage.keyValue.KeyValueStorage
import io.infinitic.common.workflows.engine.messages.WorkflowEngineEnvelope
import io.infinitic.common.workflows.engine.messages.WorkflowEngineMessage
import io.infinitic.common.workflows.engine.transport.SendToWorkflowEngine
import io.infinitic.pulsar.InfiniticWorker
import io.infinitic.pulsar.transport.PulsarMessageToProcess
import io.infinitic.workflows.engine.storage.events.NoWorkflowEventStorage
import io.infinitic.workflows.engine.storage.states.WorkflowStateKeyValueStorage
import io.infinitic.workflows.engine.transport.WorkflowEngineInputChannels
import io.infinitic.workflows.engine.transport.WorkflowEngineMessageToProcess
import io.infinitic.workflows.engine.transport.WorkflowEngineOutput
import io.infinitic.workflows.engine.worker.startWorkflowEngine
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.future.await
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.apache.pulsar.client.api.Consumer
import org.apache.pulsar.client.api.Message
import org.apache.pulsar.client.api.MessageId
import org.slf4j.Logger
import org.slf4j.LoggerFactory

typealias PulsarWorkflowEngineMessageToProcess = PulsarMessageToProcess<WorkflowEngineMessage>

const val WORKFLOW_ENGINE_PROCESSING_COROUTINE_NAME = "workflow-engine-processing"
const val WORKFLOW_ENGINE_ACKNOWLEDGING_COROUTINE_NAME = "workflow-engine-acknowledging"
const val WORKFLOW_ENGINE_PULLING_COROUTINE_NAME = "workflow-engine-pulling"

private val logger: Logger
    get() = LoggerFactory.getLogger(InfiniticWorker::class.java)

private fun logError(message: Message<WorkflowEngineEnvelope>, e: Exception) = logger.error(
    "exception on message {}:${System.getProperty("line.separator")}{}",
    message,
    e
)

fun CoroutineScope.startPulsarWorkflowEngineWorker(
    consumerCounter: Int,
    workflowEngineConsumer: Consumer<WorkflowEngineEnvelope>,
    workflowEngineOutput: WorkflowEngineOutput,
    sendToWorkflowEngineDeadLetters: SendToWorkflowEngine,
    keyValueStorage: KeyValueStorage,
    logChannel: SendChannel<WorkflowEngineMessageToProcess>?,
) = launch(Dispatchers.IO) {

    val workflowCommandsChannel = Channel<PulsarWorkflowEngineMessageToProcess>()
    val workflowEventsChannel = Channel<PulsarWorkflowEngineMessageToProcess>()
    val workflowResultsChannel = Channel<PulsarWorkflowEngineMessageToProcess>()

    // Starting Workflow Engine
    startWorkflowEngine(
        "$WORKFLOW_ENGINE_PROCESSING_COROUTINE_NAME-$consumerCounter",
        WorkflowStateKeyValueStorage(keyValueStorage),
        NoWorkflowEventStorage(),
        WorkflowEngineInputChannels(workflowCommandsChannel, workflowEventsChannel, workflowResultsChannel),
        workflowEngineOutput
    )

    fun negativeAcknowledge(pulsarId: MessageId) =
        workflowEngineConsumer.negativeAcknowledge(pulsarId)

    suspend fun acknowledge(pulsarId: MessageId) =
        workflowEngineConsumer.acknowledgeAsync(pulsarId).await()

    // coroutine dedicated to pulsar message acknowledging
    launch(CoroutineName("$WORKFLOW_ENGINE_ACKNOWLEDGING_COROUTINE_NAME-$consumerCounter")) {
        for (messageToProcess in workflowResultsChannel) {
            when (messageToProcess.exception) {
                null -> acknowledge(messageToProcess.pulsarId)
                else -> negativeAcknowledge(messageToProcess.pulsarId)
            }
            logChannel?.send(messageToProcess)
        }
    }

    // coroutine dedicated to pulsar message pulling
    launch(CoroutineName("$WORKFLOW_ENGINE_PULLING_COROUTINE_NAME-$consumerCounter")) {
        while (isActive) {
            val message: Message<WorkflowEngineEnvelope> = workflowEngineConsumer.receiveAsync().await()

            try {
                val envelope = WorkflowEngineEnvelope.fromByteArray(message.data)

                workflowCommandsChannel.send(
                    PulsarMessageToProcess(
                        message = envelope.message(),
                        pulsarId = message.messageId,
                        redeliveryCount = message.redeliveryCount
                    )
                )
            } catch (e: Exception) {
                logError(message, e)
                negativeAcknowledge(message.messageId)
            }
        }
    }
}
