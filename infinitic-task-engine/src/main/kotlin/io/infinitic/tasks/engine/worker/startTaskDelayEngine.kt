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

package io.infinitic.tasks.engine.worker

import io.infinitic.common.tasks.engine.SendToTaskEngine
import io.infinitic.tasks.engine.TaskEngine
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger(TaskEngine::class.java)

private fun logError(messageToProcess: TaskEngineMessageToProcess, e: Throwable) = logger.error(
    "taskId {} - exception on message {}: {}",
    messageToProcess.message.taskId,
    messageToProcess.message,
    e
)

fun <T : TaskEngineMessageToProcess> CoroutineScope.startTaskDelayEngine(
    coroutineName: String,
    inputChannel: ReceiveChannel<T>,
    outputChannel: SendChannel<T>,
    sendToTaskEngine: SendToTaskEngine,
) = launch(CoroutineName(coroutineName)) {

    for (messageToProcess in inputChannel) {
        try {
            sendToTaskEngine(messageToProcess.message)
        } catch (e: Throwable) {
            messageToProcess.throwable = e
            logError(messageToProcess, e)
        } finally {
            outputChannel.send(messageToProcess)
        }
    }
}
