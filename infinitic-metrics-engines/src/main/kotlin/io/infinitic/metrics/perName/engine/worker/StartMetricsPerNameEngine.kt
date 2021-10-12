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

import io.infinitic.common.clients.data.ClientName
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
import mu.KotlinLogging

private val logger = KotlinLogging.logger(MetricsPerNameEngine::class.java.name)

typealias MetricsPerNameMessageToProcess = MessageToProcess<MetricsPerNameMessage>

private fun logError(messageToProcess: MetricsPerNameMessageToProcess, e: Throwable) = logger.error(e) {
    "exception on message ${messageToProcess.message}: $e"
}

fun <T : MetricsPerNameMessageToProcess> CoroutineScope.startMetricsPerNameEngine(
    name: String,
    metricsPerNameStateStorage: MetricsPerNameStateStorage,
    inputChannel: ReceiveChannel<T>,
    outputChannel: SendChannel<T>,
    sendToMetricsGlobal: SendToMetricsGlobal
) = launch(CoroutineName(name)) {

    val metricsPerNameEngine = MetricsPerNameEngine(
        ClientName(name),
        metricsPerNameStateStorage,
        sendToMetricsGlobal
    )

    for (messageToProcess in inputChannel) {
        try {
            messageToProcess.returnValue = metricsPerNameEngine.handle(messageToProcess.message)
        } catch (e: Throwable) {
            messageToProcess.throwable = e
            logError(messageToProcess, e)
        } finally {
            outputChannel.send(messageToProcess)
        }
    }
}
