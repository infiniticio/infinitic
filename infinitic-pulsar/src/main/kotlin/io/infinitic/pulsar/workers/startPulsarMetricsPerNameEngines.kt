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
import io.infinitic.common.tasks.metrics.messages.TaskMetricsEnvelope
import io.infinitic.common.tasks.metrics.messages.TaskMetricsMessage
import io.infinitic.common.tasks.metrics.storage.TaskMetricsStateStorage
import io.infinitic.metrics.perName.engine.worker.startMetricsPerNameEngine
import io.infinitic.pulsar.transport.PulsarConsumerFactory
import io.infinitic.pulsar.transport.PulsarMessageToProcess
import io.infinitic.pulsar.transport.PulsarOutput
import io.infinitic.transport.pulsar.topics.TaskTopic
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import org.apache.pulsar.client.api.Consumer

typealias PulsarMetricsPerNameMessageToProcess = PulsarMessageToProcess<TaskMetricsMessage>

@Suppress("UNCHECKED_CAST")
fun CoroutineScope.startPulsarMetricsPerNameEngines(
    name: String,
    storage: TaskMetricsStateStorage,
    taskName: TaskName,
    consumerFactory: PulsarConsumerFactory,
    pulsarOutput: PulsarOutput
) {
    val inputChannel = Channel<PulsarMetricsPerNameMessageToProcess>()
    val outputChannel = Channel<PulsarMetricsPerNameMessageToProcess>()

    startMetricsPerNameEngine(
        "metrics-per-name: $name",
        storage,
        inputChannel = inputChannel,
        outputChannel = outputChannel,
        pulsarOutput.sendToGlobalMetrics()
    )

    // Pulsar consumer
    val consumer = consumerFactory.newConsumer(
        consumerName = name,
        taskTopic = TaskTopic.METRICS,
        taskName = taskName
    ) as Consumer<TaskMetricsEnvelope>

    // coroutine pulling pulsar messages
    pullMessages(consumer, inputChannel)

    // coroutine acknowledging pulsar messages
    acknowledgeMessages(consumer, outputChannel)
}
