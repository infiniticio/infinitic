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

import io.infinitic.common.clients.ClientFactory
import io.infinitic.common.data.Name
import io.infinitic.common.exceptions.thisShouldNotHappen
import io.infinitic.common.tasks.data.TaskName
import io.infinitic.common.tasks.executors.messages.TaskExecutorEnvelope
import io.infinitic.common.tasks.executors.messages.TaskExecutorMessage
import io.infinitic.common.workers.WorkerRegister
import io.infinitic.common.workflows.data.workflows.WorkflowName
import io.infinitic.pulsar.transport.PulsarConsumerFactory
import io.infinitic.pulsar.transport.PulsarMessageToProcess
import io.infinitic.pulsar.transport.PulsarOutput
import io.infinitic.tasks.executor.worker.startTaskExecutor
import io.infinitic.transport.pulsar.topics.TaskTopic
import io.infinitic.transport.pulsar.topics.WorkflowTaskTopic
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import org.apache.pulsar.client.api.Consumer

typealias PulsarTaskExecutorMessageToProcess = PulsarMessageToProcess<TaskExecutorMessage>

@Suppress("UNCHECKED_CAST")
fun CoroutineScope.startPulsarTaskExecutors(
    name: Name,
    concurrency: Int,
    consumerName: String,
    workerRegister: WorkerRegister,
    consumerFactory: PulsarConsumerFactory,
    output: PulsarOutput,
    clientFactory: ClientFactory
) {

    val inputChannel = Channel<PulsarTaskExecutorMessageToProcess>()
    val outputChannel = Channel<PulsarTaskExecutorMessageToProcess>()

    // launch n=concurrency coroutines running task executors
    repeat(concurrency) {
        val sendToTaskEngine = when (name) {
            is TaskName -> output.sendToTaskEngine()
            is WorkflowName -> output.sendToWorkflowTaskEngine(name)
            else -> thisShouldNotHappen()
        }

        startTaskExecutor(
            "pulsar-task-executor:$it",
            workerRegister,
            inputChannel,
            outputChannel,
            sendToTaskEngine,
            clientFactory
        )
    }

    // Pulsar consumer
    val consumer = when (name) {
        is TaskName -> consumerFactory.newConsumer(
            consumerName = consumerName,
            taskTopic = TaskTopic.EXECUTORS,
            taskName = name
        )
        is WorkflowName -> consumerFactory.newConsumer(
            consumerName = consumerName,
            workflowTaskTopic = WorkflowTaskTopic.EXECUTORS,
            workflowName = name
        )
        else -> thisShouldNotHappen()
    } as Consumer<TaskExecutorEnvelope>

    // coroutine pulling pulsar commands messages
    pullMessages(consumer, inputChannel)

    // coroutine acknowledging pulsar commands messages
    acknowledgeMessages(consumer, outputChannel)
}
