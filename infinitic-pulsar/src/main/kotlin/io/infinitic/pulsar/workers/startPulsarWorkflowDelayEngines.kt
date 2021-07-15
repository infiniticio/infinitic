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
import io.infinitic.common.workflows.engine.messages.WorkflowEngineEnvelope
import io.infinitic.pulsar.topics.TopicType
import io.infinitic.pulsar.topics.WorkflowTopic
import io.infinitic.pulsar.transport.PulsarConsumerFactory
import io.infinitic.pulsar.transport.PulsarOutput
import io.infinitic.workflows.engine.worker.startWorkflowDelayEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import org.apache.pulsar.client.api.Consumer

@Suppress("UNCHECKED_CAST")
fun CoroutineScope.startPulsarWorkflowDelayEngines(
    workflowName: WorkflowName,
    consumerName: String,
    concurrency: Int,
    consumerFactory: PulsarConsumerFactory,
    output: PulsarOutput
) {
    val inputChannel = Channel<PulsarWorkflowEngineMessageToProcess>()
    val outputChannel = Channel<PulsarWorkflowEngineMessageToProcess>()

    repeat(concurrency) {
        startWorkflowDelayEngine(
            consumerName,
            inputChannel,
            outputChannel,
            output.sendToWorkflowEngine(TopicType.EXISTING)
        )
    }

    // Pulsar consumer
    val consumer = consumerFactory.newConsumer(
        consumerName = consumerName,
        workflowTopic = WorkflowTopic.DELAYS,
        workflowName = workflowName
    ) as Consumer<WorkflowEngineEnvelope>

    // coroutine pulling pulsar events messages
    pullMessages(consumer, inputChannel)

    // coroutine acknowledging pulsar event messages
    acknowledgeMessages(consumer, outputChannel)
}
