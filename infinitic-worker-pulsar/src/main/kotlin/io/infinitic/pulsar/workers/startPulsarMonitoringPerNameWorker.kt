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

import io.infinitic.common.monitoring.perName.messages.MonitoringPerNameEngineMessage
import io.infinitic.common.monitoring.perName.messages.MonitoringPerNameEnvelope
import io.infinitic.common.serDe.kotlin.readBinary
import io.infinitic.common.storage.keyValue.KeyValueStorage
import io.infinitic.monitoring.perName.engine.storage.MonitoringPerNameStateKeyValueStorage
import io.infinitic.monitoring.perName.engine.transport.MonitoringPerNameInput
import io.infinitic.monitoring.perName.engine.worker.startMonitoringPerNameEngine
import io.infinitic.pulsar.schemas.schemaDefinition
import io.infinitic.pulsar.topics.MonitoringPerNameTopic
import io.infinitic.pulsar.transport.PulsarMessageToProcess
import io.infinitic.pulsar.transport.PulsarMonitoringPerNameOutput
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
import org.apache.pulsar.client.api.SubscriptionType

typealias PulsarMonitoringPerNameMessageToProcess = PulsarMessageToProcess<MonitoringPerNameEngineMessage>

fun CoroutineScope.startPulsarMonitoringPerNameWorker(
    pulsarClient: PulsarClient,
    keyValueStorage: KeyValueStorage,
    instancesNumber: Int = 1
) = launch(Dispatchers.IO) {

    repeat(instancesNumber) {
        val monitoringPerNameChannel = Channel<PulsarMonitoringPerNameMessageToProcess>()
        val monitoringPerNameResultsChannel = Channel<PulsarMonitoringPerNameMessageToProcess>()

        // starting monitoring per name Engine
        startMonitoringPerNameEngine(
            "monitoring-per-name-$it",
            MonitoringPerNameStateKeyValueStorage(keyValueStorage),
            MonitoringPerNameInput(monitoringPerNameChannel, monitoringPerNameResultsChannel),
            PulsarMonitoringPerNameOutput.from(pulsarClient)
        )

        // create monitoring per name consumer
        val monitoringPerNameConsumer: Consumer<MonitoringPerNameEnvelope> =
            pulsarClient.newConsumer(Schema.AVRO(schemaDefinition<MonitoringPerNameEnvelope>()))
                .topics(listOf(MonitoringPerNameTopic.name))
                .subscriptionName("monitoring-per-name-consumer-$it")
                .subscriptionType(SubscriptionType.Key_Shared)
                .subscribe()

        // coroutine dedicated to pulsar message acknowledging
        launch(CoroutineName("monitoring-per-name-message-acknowledger-$it")) {
            for (messageToProcess in monitoringPerNameResultsChannel) {
//                println("MONITORING_PER_NAME: ${messageToProcess.message}")
                if (messageToProcess.exception == null) {
                    monitoringPerNameConsumer.acknowledgeAsync(messageToProcess.messageId).await()
                } else {
                    monitoringPerNameConsumer.negativeAcknowledge(messageToProcess.messageId)
                }
            }
        }

        // coroutine dedicated to pulsar message pulling
        launch(CoroutineName("monitoring-per-name-message-puller-$it")) {
            while (isActive) {
                val message: Message<MonitoringPerNameEnvelope> = monitoringPerNameConsumer.receiveAsync().await()

                try {
                    val envelope = readBinary(message.data, MonitoringPerNameEnvelope.serializer())
                    monitoringPerNameChannel.send(PulsarMessageToProcess(envelope.message(), message.messageId))
                } catch (e: Exception) {
                    monitoringPerNameConsumer.negativeAcknowledge(message.messageId)
                    throw e
                }
            }
        }
    }
}
