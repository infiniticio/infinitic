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

import io.infinitic.common.serDe.kotlin.readBinary
import io.infinitic.common.storage.keyValue.KeyValueStorage
import io.infinitic.common.workflows.engine.messages.WorkflowEngineEnvelope
import io.infinitic.common.workflows.engine.messages.WorkflowEngineMessage
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

typealias PulsarWorkflowEngineMessageToProcess = PulsarMessageToProcess<WorkflowEngineMessage>

const val WORKFLOW_ENGINE_PROCESSING_COROUTINE_NAME = "workflow-engine-processing"
const val WORKFLOW_ENGINE_ACKNOWLEDGING_COROUTINE_NAME = "workflow-engine-acknowledging"
const val WORKFLOW_ENGINE_PULLING_COROUTINE_NAME = "workflow-engine-pulling"

fun CoroutineScope.startPulsarWorkflowEngineWorker(
    consumerCounter: Int,
    workflowEngineConsumer: Consumer<WorkflowEngineEnvelope>,
    workflowEngineOutput: WorkflowEngineOutput,
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

    // coroutine dedicated to pulsar message acknowledging
    launch(CoroutineName("$WORKFLOW_ENGINE_ACKNOWLEDGING_COROUTINE_NAME-$consumerCounter")) {
        for (messageToProcess in workflowResultsChannel) {
            if (messageToProcess.exception == null) {
                workflowEngineConsumer.acknowledgeAsync(messageToProcess.messageId).await()
            } else {
                workflowEngineConsumer.negativeAcknowledge(messageToProcess.messageId)
            }
            logChannel?.send(messageToProcess)
        }
    }

    // coroutine dedicated to pulsar message pulling
    launch(CoroutineName("$WORKFLOW_ENGINE_PULLING_COROUTINE_NAME-$consumerCounter")) {
        while (isActive) {
            val message: Message<WorkflowEngineEnvelope> = workflowEngineConsumer.receiveAsync().await()

            try {
                val envelope = readBinary(message.data, WorkflowEngineEnvelope.serializer())
                workflowCommandsChannel.send(PulsarMessageToProcess(envelope.message(), message.messageId))
            } catch (e: Exception) {
                workflowEngineConsumer.negativeAcknowledge(message.messageId)
                throw e
            }
        }
    }
}