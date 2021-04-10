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

import io.infinitic.common.metrics.global.transport.SendToMetricsGlobal
import io.infinitic.common.metrics.perName.messages.MetricsPerNameMessage
import io.infinitic.metrics.perName.engine.storage.MetricsPerNameStateStorage
import io.infinitic.metrics.perName.engine.worker.startMetricsPerNameEngine
import io.infinitic.pulsar.transport.PulsarConsumerFactory
import io.infinitic.pulsar.transport.PulsarMessageToProcess
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel

typealias PulsarMetricsPerNameMessageToProcess = PulsarMessageToProcess<MetricsPerNameMessage>

fun CoroutineScope.startPulsarMetricsPerNameEngines(
    concurrency: Int,
    consumerName: String,
    consumerFactory: PulsarConsumerFactory,
    storage: MetricsPerNameStateStorage,
    sendToMetricsGlobal: SendToMetricsGlobal
) {
    repeat(concurrency) {

        val inputChannel = Channel<PulsarMetricsPerNameMessageToProcess>()
        val outputChannel = Channel<PulsarMetricsPerNameMessageToProcess>()

        startMetricsPerNameEngine(
            "metrics-per-name-$it",
            storage,
            inputChannel = inputChannel,
            outputChannel = outputChannel,
            sendToMetricsGlobal
        )

        // Pulsar consumer
        val consumer = consumerFactory.newMetricsPerNameEngineConsumer(consumerName, it)

        // coroutine pulling pulsar messages
        pullMessages(consumer, inputChannel)

        // coroutine acknowledging pulsar messages
        acknowledgeMessages(consumer, outputChannel)
    }
}
