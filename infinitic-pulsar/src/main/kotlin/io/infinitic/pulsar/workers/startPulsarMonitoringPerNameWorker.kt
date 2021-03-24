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

import io.infinitic.common.monitoring.perName.messages.MonitoringPerNameEnvelope
import io.infinitic.common.monitoring.perName.messages.MonitoringPerNameMessage
import io.infinitic.common.monitoring.perName.state.MonitoringPerNameState
import io.infinitic.common.monitoring.perName.transport.SendToMonitoringPerName
import io.infinitic.common.storage.keyValue.KeyValueCache
import io.infinitic.common.storage.keyValue.KeyValueStorage
import io.infinitic.common.workers.singleThreadedContext
import io.infinitic.monitoring.perName.engine.MonitoringPerNameEngine
import io.infinitic.monitoring.perName.engine.storage.MonitoringPerNameStateKeyValueStorage
import io.infinitic.monitoring.perName.engine.transport.MonitoringPerNameOutput
import io.infinitic.pulsar.InfiniticWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.future.await
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.apache.pulsar.client.api.Consumer
import org.apache.pulsar.client.api.Message
import org.apache.pulsar.client.api.MessageId
import org.slf4j.Logger
import org.slf4j.LoggerFactory

const val MONITORING_PER_NAME_THREAD_NAME = "monitoring-per-name"

private val logger: Logger
    get() = LoggerFactory.getLogger(InfiniticWorker::class.java)

private fun logError(message: Message<MonitoringPerNameEnvelope>, e: Exception) = logger.error(
    "exception on message {}:${System.getProperty("line.separator")}{}",
    message,
    e
)

private fun logError(message: MonitoringPerNameMessage, e: Exception) = logger.error(
    "taskName {} - exception on message {}:${System.getProperty("line.separator")}{}",
    message.taskName,
    message,
    e
)

fun CoroutineScope.startPulsarMonitoringPerNameWorker(
    consumerCounter: Int,
    monitoringPerNameConsumer: Consumer<MonitoringPerNameEnvelope>,
    monitoringPerNameOutput: MonitoringPerNameOutput,
    sendToMonitoringPerNameDeadLetters: SendToMonitoringPerName,
    keyValueStorage: KeyValueStorage,
    keyValueCache: KeyValueCache<MonitoringPerNameState>
) = launch(singleThreadedContext("$MONITORING_PER_NAME_THREAD_NAME-$consumerCounter")) {

    val monitoringPerNameEngine = MonitoringPerNameEngine(
        MonitoringPerNameStateKeyValueStorage(keyValueStorage, keyValueCache),
        monitoringPerNameOutput
    )

    fun negativeAcknowledge(pulsarId: MessageId) =
        monitoringPerNameConsumer.negativeAcknowledge(pulsarId)

    suspend fun acknowledge(pulsarId: MessageId) =
        monitoringPerNameConsumer.acknowledgeAsync(pulsarId).await()

    while (isActive) {
        val pulsarMessage = monitoringPerNameConsumer.receiveAsync().await()

        val message = try {
            MonitoringPerNameEnvelope.fromByteArray(pulsarMessage.data).message()
        } catch (e: Exception) {
            logError(pulsarMessage, e)
            negativeAcknowledge(pulsarMessage.messageId)

            null
        }

        message?.let {
            try {
                monitoringPerNameEngine.handle(it)

                acknowledge(pulsarMessage.messageId)
            } catch (e: Exception) {
                logError(message, e)
                negativeAcknowledge(pulsarMessage.messageId)
            }
        }
    }
}
