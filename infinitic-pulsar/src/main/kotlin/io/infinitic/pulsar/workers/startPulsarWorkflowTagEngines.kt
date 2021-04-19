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

import io.infinitic.common.workflows.data.workflows.WorkflowName
import io.infinitic.common.workflows.tags.messages.WorkflowTagEngineMessage
import io.infinitic.pulsar.topics.TopicType
import io.infinitic.pulsar.transport.PulsarConsumerFactory
import io.infinitic.pulsar.transport.PulsarMessageToProcess
import io.infinitic.pulsar.transport.PulsarOutput
import io.infinitic.tags.workflows.storage.WorkflowTagStorage
import io.infinitic.tags.workflows.worker.startWorkflowTagEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel

typealias PulsarWorkflowTagEngineMessageToProcess = PulsarMessageToProcess<WorkflowTagEngineMessage>

fun CoroutineScope.startPulsarWorkflowTagEngines(
    workflowName: WorkflowName,
    consumerName: String,
    concurrency: Int,
    storage: WorkflowTagStorage,
    consumerFactory: PulsarConsumerFactory,
    output: PulsarOutput
) {
    repeat(concurrency) {

        val eventsInputChannel = Channel<PulsarWorkflowTagEngineMessageToProcess>()
        val eventsOutputChannel = Channel<PulsarWorkflowTagEngineMessageToProcess>()
        val commandsInputChannel = Channel<PulsarWorkflowTagEngineMessageToProcess>()
        val commandsOutputChannel = Channel<PulsarWorkflowTagEngineMessageToProcess>()

        startWorkflowTagEngine(
            "workflow-tag-engine:$it",
            storage,
            eventsInputChannel = eventsInputChannel,
            eventsOutputChannel = eventsOutputChannel,
            commandsInputChannel = commandsInputChannel,
            commandsOutputChannel = commandsOutputChannel,
            output.sendToWorkflowEngine(TopicType.COMMANDS)
        )

        // Pulsar consumers
        val eventsConsumer = consumerFactory.newWorkflowTagEngineConsumer(
            consumerName = "$consumerName:$it",
            topicType = TopicType.EVENTS,
            workflowName = workflowName
        )
        val commandsConsumer = consumerFactory.newWorkflowTagEngineConsumer(
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