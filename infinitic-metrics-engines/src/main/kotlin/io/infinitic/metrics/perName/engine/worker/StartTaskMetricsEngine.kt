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

import io.infinitic.common.data.ClientName
import io.infinitic.common.metrics.global.SendToGlobalMetrics
import io.infinitic.common.tasks.metrics.messages.TaskMetricsMessage
import io.infinitic.common.tasks.metrics.storage.TaskMetricsStateStorage
import io.infinitic.common.workers.MessageToProcess
import io.infinitic.metrics.perName.engine.TaskMetricsEngine
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.launch
import mu.KotlinLogging

private val logger = KotlinLogging.logger(TaskMetricsEngine::class.java.name)

typealias MetricsPerNameMessageToProcess = MessageToProcess<TaskMetricsMessage>

private fun logError(messageToProcess: MetricsPerNameMessageToProcess, e: Throwable) = logger.error(e) {
    "exception on message ${messageToProcess.message}: $e"
}

fun <T : MetricsPerNameMessageToProcess> CoroutineScope.startMetricsPerNameEngine(
    name: String,
    taskMetricsStateStorage: TaskMetricsStateStorage,
    inputChannel: ReceiveChannel<T>,
    outputChannel: SendChannel<T>,
    sendToGlobalMetrics: SendToGlobalMetrics
) = launch(CoroutineName(name)) {

    val taskMetricsEngine = TaskMetricsEngine(
        ClientName(name),
        taskMetricsStateStorage,
        sendToGlobalMetrics
    )

    for (messageToProcess in inputChannel) {
        try {
            messageToProcess.returnValue = taskMetricsEngine.handle(messageToProcess.message)
        } catch (e: Throwable) {
            messageToProcess.throwable = e
            logError(messageToProcess, e)
        } finally {
            outputChannel.send(messageToProcess)
        }
    }
}
