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
import io.infinitic.common.monitoring.perName.transport.SendToMonitoringPerName
import io.infinitic.common.storage.keyValue.KeyValueStorage
import io.infinitic.monitoring.perName.engine.storage.MonitoringPerNameStateKeyValueStorage
import io.infinitic.monitoring.perName.engine.transport.MonitoringPerNameInputChannels
import io.infinitic.monitoring.perName.engine.transport.MonitoringPerNameMessageToProcess
import io.infinitic.monitoring.perName.engine.transport.MonitoringPerNameOutput
import io.infinitic.monitoring.perName.engine.worker.startMonitoringPerNameEngine
import io.infinitic.pulsar.InfiniticWorker
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
import org.apache.pulsar.client.api.MessageId
import org.slf4j.Logger
import org.slf4j.LoggerFactory

typealias PulsarMonitoringPerNameMessageToProcess = PulsarMessageToProcess<MonitoringPerNameEngineMessage>

const val MONITORING_PER_NAME_PROCESSING_COROUTINE_NAME = "monitoring-per-name-processing"
const val MONITORING_PER_NAME_ACKNOWLEDGING_COROUTINE_NAME = "monitoring-per-name-acknowledging"
const val MONITORING_PER_NAME_PULLING_COROUTINE_NAME = "monitoring-per-name-pulling"

private val logger: Logger
    get() = LoggerFactory.getLogger(InfiniticWorker::class.java)

private fun logError(message: Message<MonitoringPerNameEnvelope>, e: Exception) = logger.error(
    "exception on message {}:${System.getProperty("line.separator")}{}",
    message,
    e
)

fun CoroutineScope.startPulsarMonitoringPerNameWorker(
    consumerCounter: Int,
    monitoringPerNameConsumer: Consumer<MonitoringPerNameEnvelope>,
    monitoringPerNameOutput: MonitoringPerNameOutput,
    sendToMonitoringPerNameDeadLetters: SendToMonitoringPerName,
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

    fun negativeAcknowledge(pulsarId: MessageId) =
        monitoringPerNameConsumer.negativeAcknowledge(pulsarId)

    suspend fun acknowledge(pulsarId: MessageId) =
        monitoringPerNameConsumer.acknowledgeAsync(pulsarId).await()

    // coroutine dedicated to pulsar message acknowledging
    launch(CoroutineName("$MONITORING_PER_NAME_ACKNOWLEDGING_COROUTINE_NAME-$consumerCounter")) {
        for (messageToProcess in monitoringPerNameResultsChannel) {
            when (messageToProcess.exception) {
                null -> acknowledge(messageToProcess.pulsarId)
                else -> negativeAcknowledge(messageToProcess.pulsarId)
            }
            logChannel?.send(messageToProcess)
        }
    }

    // coroutine dedicated to pulsar message pulling
    launch(CoroutineName("$MONITORING_PER_NAME_PULLING_COROUTINE_NAME-$consumerCounter")) {
        while (isActive) {
            val message: Message<MonitoringPerNameEnvelope> = monitoringPerNameConsumer.receiveAsync().await()

            try {
                val envelope = MonitoringPerNameEnvelope.fromByteArray(message.data)
                monitoringPerNameChannel.send(
                    PulsarMessageToProcess(
                        message = envelope.message(),
                        pulsarId = message.messageId,
                        redeliveryCount = message.redeliveryCount
                    )
                )
            } catch (e: Exception) {
                logError(message, e)
                negativeAcknowledge(message.messageId)
            }
        }
    }
}
