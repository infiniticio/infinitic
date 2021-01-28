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
import io.infinitic.common.monitoring.global.transport.SendToMonitoringGlobal
import io.infinitic.common.storage.keyValue.KeyValueStorage
import io.infinitic.common.workers.singleThreadedContext
import io.infinitic.monitoring.global.engine.MonitoringGlobalEngine
import io.infinitic.monitoring.global.engine.storage.MonitoringGlobalStateKeyValueStorage
import io.infinitic.pulsar.InfiniticWorker
import io.infinitic.pulsar.transport.PulsarMessageToProcess
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.future.await
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.apache.pulsar.client.api.Consumer
import org.apache.pulsar.client.api.Message
import org.apache.pulsar.client.api.MessageId
import org.slf4j.Logger
import org.slf4j.LoggerFactory

typealias PulsarMonitoringGlobalMessageToProcess = PulsarMessageToProcess<MonitoringGlobalMessage>

const val MONITORING_GLOBAL_THREAD_NAME = "monitoring-global"

private val logger: Logger
    get() = LoggerFactory.getLogger(InfiniticWorker::class.java)

private fun logError(message: Message<MonitoringGlobalEnvelope>, e: Exception) = logger.error(
    "exception on message {}:${System.getProperty("line.separator")}{}",
    message,
    e
)

private fun logError(message: MonitoringGlobalMessage, e: Exception) = logger.error(
    "exception on message {}:${System.getProperty("line.separator")}{}",
    message,
    e
)

fun CoroutineScope.startPulsarMonitoringGlobalWorker(
    monitoringGlobalConsumer: Consumer<MonitoringGlobalEnvelope>,
    sendToMonitoringGlobalDeadLetters: SendToMonitoringGlobal,
    keyValueStorage: KeyValueStorage
) = launch(singleThreadedContext(MONITORING_GLOBAL_THREAD_NAME)) {

    val monitoringGlobalEngine = MonitoringGlobalEngine(
        MonitoringGlobalStateKeyValueStorage(keyValueStorage)
    )

    fun negativeAcknowledge(pulsarId: MessageId) =
        monitoringGlobalConsumer.negativeAcknowledge(pulsarId)

    suspend fun acknowledge(pulsarId: MessageId) =
        monitoringGlobalConsumer.acknowledgeAsync(pulsarId).await()

    while (isActive) {
        val pulsarMessage = monitoringGlobalConsumer.receiveAsync().await()

        val message = try {
            MonitoringGlobalEnvelope.fromByteArray(pulsarMessage.data).message()
        } catch (e: Exception) {
            logError(pulsarMessage, e)
            negativeAcknowledge(pulsarMessage.messageId)

            null
        }

        message?.let {
            try {
                monitoringGlobalEngine.handle(it)

                acknowledge(pulsarMessage.messageId)
            } catch (e: Exception) {
                logError(message, e)
                negativeAcknowledge(pulsarMessage.messageId)
            }
        }
    }
}
