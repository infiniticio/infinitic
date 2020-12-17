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
import io.infinitic.pulsar.consumers.ConsumerFactory
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

fun CoroutineScope.startPulsarWorkflowEngineWorker(
    consumerFactory: ConsumerFactory,
    workflowEngineOutput: WorkflowEngineOutput,
    keyValueStorage: KeyValueStorage,
    logChannel: SendChannel<WorkflowEngineMessageToProcess>?,
    instancesNumber: Int = 1
) = launch(Dispatchers.IO) {

    repeat(instancesNumber) {
        val workflowCommandsChannel = Channel<PulsarWorkflowEngineMessageToProcess>()
        val workflowEventsChannel = Channel<PulsarWorkflowEngineMessageToProcess>()
        val workflowResultsChannel = Channel<PulsarWorkflowEngineMessageToProcess>()

        // Starting Workflow Engine
        startWorkflowEngine(
            "workflow-engine-$it",
            WorkflowStateKeyValueStorage(keyValueStorage),
            NoWorkflowEventStorage(),
            WorkflowEngineInputChannels(workflowCommandsChannel, workflowEventsChannel, workflowResultsChannel),
            workflowEngineOutput
        )

        // create workflow engine consumer
        val workflowEngineConsumer: Consumer<WorkflowEngineEnvelope> = consumerFactory
            .newWorkflowEngineConsumer(
                if (instancesNumber > 1) "$it" else null
            )

        // coroutine dedicated to pulsar message acknowledging
        launch(CoroutineName("workflow-engine-message-acknowledger-$it")) {
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
        launch(CoroutineName("workflow-engine-message-puller-$it")) {
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
}
