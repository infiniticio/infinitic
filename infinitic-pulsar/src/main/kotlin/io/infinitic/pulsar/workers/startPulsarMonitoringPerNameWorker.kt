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
import io.infinitic.monitoring.perName.engine.transport.MonitoringPerNameInputChannels
import io.infinitic.monitoring.perName.engine.transport.MonitoringPerNameMessageToProcess
import io.infinitic.monitoring.perName.engine.transport.MonitoringPerNameOutput
import io.infinitic.monitoring.perName.engine.worker.startMonitoringPerNameEngine
import io.infinitic.pulsar.transport.PulsarMessageToProcess
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.future.await
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.apache.pulsar.client.api.Consumer
import org.apache.pulsar.client.api.Message

typealias PulsarMonitoringPerNameMessageToProcess = PulsarMessageToProcess<MonitoringPerNameEngineMessage>

const val MONITORING_PER_NAME_PROCESSING_COROUTINE_NAME = "monitoring-per-name-processing"
const val MONITORING_PER_NAME_ACKNOWLEDGING_COROUTINE_NAME = "monitoring-per-name-acknowledging"
const val MONITORING_PER_NAME_PULLING_COROUTINE_NAME = "monitoring-per-name-pulling"

fun CoroutineScope.startPulsarMonitoringPerNameWorker(
    consumerCounter: Int,
    monitoringPerNameConsumer: Consumer<MonitoringPerNameEnvelope>,
    monitoringPerNameOutput: MonitoringPerNameOutput,
    keyValueStorage: KeyValueStorage,
    logChannel: SendChannel<MonitoringPerNameMessageToProcess>?,
) = launch(Dispatchers.IO) {

    val monitoringPerNameChannel = Channel<PulsarMonitoringPerNameMessageToProcess>()
    val monitoringPerNameResultsChannel = Channel<PulsarMonitoringPerNameMessageToProcess>()

    // starting monitoring per name Engine
    startMonitoringPerNameEngine(
        "$MONITORING_PER_NAME_PROCESSING_COROUTINE_NAME-$consumerCounter",
        MonitoringPerNameStateKeyValueStorage(keyValueStorage),
        MonitoringPerNameInputChannels(monitoringPerNameChannel, monitoringPerNameResultsChannel),
        monitoringPerNameOutput
    )

    // coroutine dedicated to pulsar message acknowledging
    launch(CoroutineName("$MONITORING_PER_NAME_ACKNOWLEDGING_COROUTINE_NAME-$consumerCounter")) {
        for (messageToProcess in monitoringPerNameResultsChannel) {
            if (messageToProcess.exception == null) {
                monitoringPerNameConsumer.acknowledgeAsync(messageToProcess.messageId).await()
            } else {
                monitoringPerNameConsumer.negativeAcknowledge(messageToProcess.messageId)
            }
            logChannel?.send(messageToProcess)
        }
    }

    // coroutine dedicated to pulsar message pulling
    launch(CoroutineName("$MONITORING_PER_NAME_PULLING_COROUTINE_NAME-$consumerCounter")) {
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
