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

import io.infinitic.common.data.Name
import io.infinitic.common.exceptions.thisShouldNotHappen
import io.infinitic.common.tasks.data.TaskName
import io.infinitic.common.tasks.engine.messages.TaskEngineEnvelope
import io.infinitic.common.tasks.engine.messages.TaskEngineMessage
import io.infinitic.common.tasks.engine.storage.TaskStateStorage
import io.infinitic.common.workflows.data.workflows.WorkflowName
import io.infinitic.pulsar.transport.PulsarConsumerFactory
import io.infinitic.pulsar.transport.PulsarMessageToProcess
import io.infinitic.pulsar.transport.PulsarOutput
import io.infinitic.tasks.engine.worker.startTaskEngine
import io.infinitic.transport.pulsar.topics.TaskTopic
import io.infinitic.transport.pulsar.topics.WorkflowTaskTopic
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import org.apache.pulsar.client.api.Consumer

typealias PulsarTaskEngineMessageToProcess = PulsarMessageToProcess<TaskEngineMessage>

@Suppress("UNCHECKED_CAST")
fun CoroutineScope.startPulsarTaskEngines(
    name: String,
    concurrency: Int,
    storage: TaskStateStorage,
    jobName: Name,
    consumerFactory: PulsarConsumerFactory,
    output: PulsarOutput
) {
    repeat(concurrency) {

        val inputChannel = Channel<PulsarTaskEngineMessageToProcess>()
        val outputChannel = Channel<PulsarTaskEngineMessageToProcess>()

        startTaskEngine(
            "task-engine-$it: $name",
            storage,
            inputChannel = inputChannel,
            outputChannel = outputChannel,
            output.sendToClient(),
            output.sendToTaskTagEngine(),
            output.sendToTaskEngineAfter(jobName),
            output.sendToWorkflowEngine(),
            output.sendToTaskExecutors(jobName),
            output.sendToTaskMetrics()
        )

        // Pulsar consumers
        val consumer = when (jobName) {
            is TaskName -> consumerFactory.newConsumer(
                consumerName = "$name:$it",
                taskTopic = TaskTopic.ENGINE,
                taskName = jobName
            )
            is WorkflowName -> consumerFactory.newConsumer(
                consumerName = "$name:$it",
                workflowTaskTopic = WorkflowTaskTopic.ENGINE,
                workflowName = jobName
            )
            else -> thisShouldNotHappen()
        } as Consumer<TaskEngineEnvelope>

        // coroutine pulling pulsar messages
        pullMessages(consumer, inputChannel)

        // coroutine acknowledging pulsar messages
        acknowledgeMessages(consumer, outputChannel)
    }
}
