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
import io.infinitic.common.workflows.engine.messages.WorkflowEngineMessage
import io.infinitic.pulsar.topics.TopicType
import io.infinitic.pulsar.transport.PulsarConsumerFactory
import io.infinitic.pulsar.transport.PulsarMessageToProcess
import io.infinitic.pulsar.transport.PulsarOutput
import io.infinitic.workflows.engine.storage.WorkflowStateStorage
import io.infinitic.workflows.engine.worker.startWorkflowEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel

typealias PulsarWorkflowEngineMessageToProcess = PulsarMessageToProcess<WorkflowEngineMessage>

fun CoroutineScope.startPulsarWorkflowEngines(
    workflowName: WorkflowName,
    consumerName: String,
    concurrency: Int,
    storage: WorkflowStateStorage,
    consumerFactory: PulsarConsumerFactory,
    output: PulsarOutput
) {
    repeat(concurrency) {

        val eventsInputChannel = Channel<PulsarWorkflowEngineMessageToProcess>()
        val eventsOutputChannel = Channel<PulsarWorkflowEngineMessageToProcess>()
        val commandsInputChannel = Channel<PulsarWorkflowEngineMessageToProcess>()
        val commandsOutputChannel = Channel<PulsarWorkflowEngineMessageToProcess>()

        val taskEngine = output.sendToTaskEngine(TopicType.COMMANDS)
        val workflowTaskEngine = output.sendToTaskEngine(TopicType.COMMANDS, workflowName)

        startWorkflowEngine(
            "workflow-engine:$it",
            storage,
            eventsInputChannel = eventsInputChannel,
            eventsOutputChannel = eventsOutputChannel,
            commandsInputChannel = commandsInputChannel,
            commandsOutputChannel = commandsOutputChannel,
            output.sendToClient(),
            output.sendToWorkflowTagEngine(TopicType.EVENTS),
            sendToTaskEngine = { msg, after -> if (msg.isWorkflowTask()) workflowTaskEngine(msg, after) else taskEngine(msg, after) },
            output.sendToWorkflowEngine(TopicType.EVENTS)
        )

        // Pulsar consumers
        val eventsConsumer = consumerFactory.newWorkflowEngineConsumer(
            consumerName = "$consumerName:$it",
            topicType = TopicType.EVENTS,
            workflowName = workflowName
        )
        val commandsConsumer = consumerFactory.newWorkflowEngineConsumer(
            consumerName = "$consumerName:$it",
            topicType = TopicType.COMMANDS,
            workflowName = workflowName
        )

        // coroutine pulling pulsar events messages
        pullMessages(eventsConsumer, eventsInputChannel)

        // coroutine pulling pulsar commands messages
        pullMessages(commandsConsumer, commandsInputChannel)

        // coroutine acknowledging pulsar event messages
        acknowledgeMessages(eventsConsumer, eventsOutputChannel)

        // coroutine acknowledging pulsar commands messages
        acknowledgeMessages(commandsConsumer, commandsOutputChannel)
    }
}
