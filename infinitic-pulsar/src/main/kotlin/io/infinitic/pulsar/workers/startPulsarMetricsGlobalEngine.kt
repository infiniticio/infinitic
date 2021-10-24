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

import io.infinitic.common.metrics.global.messages.MetricsGlobalEnvelope
import io.infinitic.common.metrics.global.messages.MetricsGlobalMessage
import io.infinitic.metrics.global.engine.storage.MetricsGlobalStateStorage
import io.infinitic.metrics.global.engine.worker.startMetricsGlobalEngine
import io.infinitic.pulsar.topics.GlobalTopic
import io.infinitic.pulsar.transport.PulsarConsumerFactory
import io.infinitic.pulsar.transport.PulsarMessageToProcess
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import org.apache.pulsar.client.api.Consumer

typealias PulsarMetricsGlobalMessageToProcess = PulsarMessageToProcess<MetricsGlobalMessage>

@Suppress("UNCHECKED_CAST")
fun CoroutineScope.startPulsarMetricsGlobalEngine(
    name: String,
    storage: MetricsGlobalStateStorage,
    consumerFactory: PulsarConsumerFactory
) {

    val inputChannel = Channel<PulsarMetricsGlobalMessageToProcess>()
    val outputChannel = Channel<PulsarMetricsGlobalMessageToProcess>()

    startMetricsGlobalEngine(
        "metrics-global-engine: $name",
        storage,
        inputChannel = inputChannel,
        outputChannel = outputChannel
    )

    val consumer = consumerFactory.newConsumer(name, GlobalTopic.METRICS) as Consumer<MetricsGlobalEnvelope>

    // coroutine pulling pulsar messages
    pullMessages(consumer, inputChannel)

    // coroutine acknowledging pulsar messages
    acknowledgeMessages(consumer, outputChannel)
}
