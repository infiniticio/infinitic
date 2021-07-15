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

import io.infinitic.common.workflows.data.workflowTasks.isWorkflowTask
import io.infinitic.common.workflows.data.workflows.WorkflowName
import io.infinitic.common.workflows.engine.messages.WorkflowEngineEnvelope
import io.infinitic.common.workflows.engine.messages.WorkflowEngineMessage
import io.infinitic.pulsar.topics.TopicType
import io.infinitic.pulsar.topics.WorkflowTopic
import io.infinitic.pulsar.transport.PulsarConsumerFactory
import io.infinitic.pulsar.transport.PulsarMessageToProcess
import io.infinitic.pulsar.transport.PulsarOutput
import io.infinitic.workflows.engine.storage.WorkflowStateStorage
import io.infinitic.workflows.engine.worker.startWorkflowEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import org.apache.pulsar.client.api.Consumer

typealias PulsarWorkflowEngineMessageToProcess = PulsarMessageToProcess<WorkflowEngineMessage>

@Suppress("UNCHECKED_CAST")
fun CoroutineScope.startPulsarWorkflowEngines(
    workflowName: WorkflowName,
    consumerName: String,
    concurrency: Int,
    storage: WorkflowStateStorage,
    consumerFactory: PulsarConsumerFactory,
    output: PulsarOutput
) {
    repeat(concurrency) { count ->

        val eventsInputChannel = Channel<PulsarWorkflowEngineMessageToProcess>()
        val eventsOutputChannel = Channel<PulsarWorkflowEngineMessageToProcess>()
        val commandsInputChannel = Channel<PulsarWorkflowEngineMessageToProcess>()
        val commandsOutputChannel = Channel<PulsarWorkflowEngineMessageToProcess>()

        val taskEngine = output.sendToTaskEngine(TopicType.NEW)
        val workflowTaskEngine = output.sendToTaskEngine(TopicType.NEW, workflowName)

        startWorkflowEngine(
            "workflow-engine:$count",
            storage,
            eventsInputChannel = eventsInputChannel,
            eventsOutputChannel = eventsOutputChannel,
            commandsInputChannel = commandsInputChannel,
            commandsOutputChannel = commandsOutputChannel,
            output.sendToClient(),
            output.sendToTaskTagEngine(TopicType.NEW),
            sendToTaskEngine = { if (it.isWorkflowTask()) workflowTaskEngine(it) else taskEngine(it) },
            output.sendToWorkflowTagEngine(TopicType.EXISTING),
            output.sendToWorkflowEngine(TopicType.EXISTING),
            output.sendToWorkflowEngineAfter()
        )

        // Pulsar consumers
        val existingConsumer = consumerFactory.newConsumer(
            consumerName = "$consumerName:$count",
            workflowTopic = WorkflowTopic.ENGINE_EXISTING,
            workflowName = workflowName
        ) as Consumer<WorkflowEngineEnvelope>

        val newConsumer = consumerFactory.newConsumer(
            consumerName = "$consumerName:$count",
            workflowTopic = WorkflowTopic.ENGINE_NEW,
            workflowName = workflowName
        ) as Consumer<WorkflowEngineEnvelope>

        // coroutine pulling pulsar events messages
        pullMessages(existingConsumer, eventsInputChannel)

        // coroutine pulling pulsar commands messages
        pullMessages(newConsumer, commandsInputChannel)

        // coroutine acknowledging pulsar event messages
        acknowledgeMessages(existingConsumer, eventsOutputChannel)

        // coroutine acknowledging pulsar commands messages
        acknowledgeMessages(newConsumer, commandsOutputChannel)
    }
}
