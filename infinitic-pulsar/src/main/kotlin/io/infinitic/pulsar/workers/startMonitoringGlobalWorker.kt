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

import io.infinitic.common.monitoring.global.messages.MonitoringGlobalEnvelope
import io.infinitic.common.monitoring.global.messages.MonitoringGlobalMessage
import io.infinitic.common.serDe.kotlin.readBinary
import io.infinitic.common.storage.keyValue.KeyValueStorage
import io.infinitic.monitoring.global.engine.storage.MonitoringGlobalStateKeyValueStorage
import io.infinitic.monitoring.global.engine.transport.MonitoringGlobalInput
import io.infinitic.monitoring.global.engine.worker.startMonitoringGlobalEngine
import io.infinitic.pulsar.schemas.schemaDefinition
import io.infinitic.pulsar.topics.MonitoringGlobalTopic
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.future.await
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.apache.pulsar.client.api.Consumer
import org.apache.pulsar.client.api.Message
import org.apache.pulsar.client.api.PulsarClient
import org.apache.pulsar.client.api.Schema

typealias PulsarMonitoringGlobalMessageToProcess = PulsarMessageToProcess<MonitoringGlobalMessage>

fun CoroutineScope.startMonitoringGlobalWorker(
    pulsarClient: PulsarClient,
    keyValueStorage: KeyValueStorage
) = launch(Dispatchers.IO) {

    val monitoringGlobalChannel = Channel<PulsarMonitoringGlobalMessageToProcess>()
    val monitoringGlobalResultsChannel = Channel<PulsarMonitoringGlobalMessageToProcess>()

    // starting monitoring global engine
    startMonitoringGlobalEngine(
        "monitoring-global",
        MonitoringGlobalStateKeyValueStorage(keyValueStorage),
        MonitoringGlobalInput(monitoringGlobalChannel, monitoringGlobalResultsChannel)
    )

    // create monitoring global consumer
    val monitoringGlobalConsumer: Consumer<MonitoringGlobalEnvelope> =
        pulsarClient.newConsumer(Schema.AVRO(schemaDefinition<MonitoringGlobalEnvelope>()))
            .topics(listOf(MonitoringGlobalTopic.name))
            .subscriptionName("monitoring-global-consumer")
            .subscribe()

    // coroutine dedicated to pulsar message acknowledging
    launch(CoroutineName("monitoring-global-message-acknowledger")) {
        for (messageToProcess in monitoringGlobalResultsChannel) {
            if (messageToProcess.exception == null) {
                monitoringGlobalConsumer.acknowledgeAsync(messageToProcess.messageId).await()
            } else {
                monitoringGlobalConsumer.negativeAcknowledge(messageToProcess.messageId)
            }
        }
    }

    // coroutine dedicated to pulsar message pulling
    launch(CoroutineName("monitoring-global-message-puller")) {
        while (isActive) {
            val message: Message<MonitoringGlobalEnvelope> = monitoringGlobalConsumer.receiveAsync().await()

            try {
                val envelope = readBinary(message.data, MonitoringGlobalEnvelope.serializer())
                monitoringGlobalChannel.send(PulsarMessageToProcess(envelope.message(), message.messageId))
            } catch (e: Exception) {
                monitoringGlobalConsumer.negativeAcknowledge(message.messageId)
                throw e
            }
        }
    }
}
