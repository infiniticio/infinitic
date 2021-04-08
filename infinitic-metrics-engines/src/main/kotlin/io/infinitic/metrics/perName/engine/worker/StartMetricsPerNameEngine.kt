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

package io.infinitic.metrics.perName.engine.worker

import io.infinitic.common.metrics.global.transport.SendToMetricsGlobal
import io.infinitic.common.metrics.perName.messages.MetricsPerNameMessage
import io.infinitic.common.workers.MessageToProcess
import io.infinitic.metrics.perName.engine.MetricsPerNameEngine
import io.infinitic.metrics.perName.engine.storage.MetricsPerNameStateStorage
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.launch
import org.slf4j.Logger
import org.slf4j.LoggerFactory

private val logger: Logger
    get() = LoggerFactory.getLogger(MetricsPerNameEngine::class.java)

typealias MetricsPerNameMessageToProcess = MessageToProcess<MetricsPerNameMessage>

private fun logError(messageToProcess: MetricsPerNameMessageToProcess, e: Exception) = logger.error(
    "taskName {} - exception on message {}:${System.getProperty("line.separator")}{}",
    messageToProcess.message.taskName,
    messageToProcess.message,
    e
)

fun <T : MetricsPerNameMessageToProcess> CoroutineScope.startMetricsPerNameEngine(
    coroutineName: String,
    metricsPerNameStateStorage: MetricsPerNameStateStorage,
    metricsPerNameChannel: ReceiveChannel<T>,
    logChannel: SendChannel<T>,
    sendToMetricsGlobal: SendToMetricsGlobal
) = launch(CoroutineName(coroutineName)) {

    val monitoringPerNameEngine = MetricsPerNameEngine(
        metricsPerNameStateStorage,
        sendToMetricsGlobal
    )

    for (messageToProcess in metricsPerNameChannel) {
        try {
            messageToProcess.returnValue = monitoringPerNameEngine.handle(messageToProcess.message)
        } catch (e: Exception) {
            messageToProcess.exception = e
            logError(messageToProcess, e)
        } finally {
            logChannel.send(messageToProcess)
        }
    }
}
