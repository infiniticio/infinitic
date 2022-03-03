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

import io.infinitic.common.tasks.data.TaskName
import io.infinitic.common.tasks.tags.messages.TaskTagEnvelope
import io.infinitic.common.tasks.tags.messages.TaskTagMessage
import io.infinitic.common.tasks.tags.storage.TaskTagStorage
import io.infinitic.pulsar.transport.PulsarConsumerFactory
import io.infinitic.pulsar.transport.PulsarMessageToProcess
import io.infinitic.pulsar.transport.PulsarOutput
import io.infinitic.tags.tasks.worker.startTaskTagEngine
import io.infinitic.transport.pulsar.topics.TaskTopic
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import org.apache.pulsar.client.api.Consumer

typealias PulsarTaskTagEngineMessageToProcess = PulsarMessageToProcess<TaskTagMessage>

@Suppress("UNCHECKED_CAST")
fun CoroutineScope.startPulsarTaskTagEngines(
    name: String,
    concurrency: Int,
    storage: TaskTagStorage,
    taskName: TaskName,
    consumerFactory: PulsarConsumerFactory,
    output: PulsarOutput
) {
    repeat(concurrency) {

        val inputChannel = Channel<PulsarTaskTagEngineMessageToProcess>()
        val outputChannel = Channel<PulsarTaskTagEngineMessageToProcess>()

        startTaskTagEngine(
            "task-tag-engine-$it: $name",
            storage,
            inputChannel = inputChannel,
            outputChannel = outputChannel,
            output.sendToTaskEngine(),
            output.sendToClient()
        )

        // Pulsar consumers
        val consumer = consumerFactory.newConsumer(
            consumerName = "$name:$it",
            taskTopic = TaskTopic.TAG,
            taskName = taskName
        ) as Consumer<TaskTagEnvelope>

        // coroutine pulling pulsar messages
        pullMessages(consumer, inputChannel)

        // coroutine acknowledging pulsar event messages
        acknowledgeMessages(consumer, outputChannel)
    }
}
