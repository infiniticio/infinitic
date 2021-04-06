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

import io.infinitic.common.clients.transport.SendToClient
import io.infinitic.common.metrics.perName.transport.SendToMetricsPerName
import io.infinitic.common.tags.transport.SendToTagEngine
import io.infinitic.common.tasks.engine.messages.TaskEngineMessage
import io.infinitic.common.tasks.engine.transport.SendToTaskEngine
import io.infinitic.common.tasks.executors.SendToTaskExecutors
import io.infinitic.common.workers.MessageToProcess
import io.infinitic.common.workflows.engine.transport.SendToWorkflowEngine
import io.infinitic.tasks.engine.TaskEngine
import io.infinitic.tasks.engine.storage.TaskStateStorage
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.launch
import kotlinx.coroutines.selects.select
import org.slf4j.Logger
import org.slf4j.LoggerFactory

private val logger: Logger
    get() = LoggerFactory.getLogger(TaskEngine::class.java)

typealias TaskEngineMessageToProcess = MessageToProcess<TaskEngineMessage>

private fun logError(messageToProcess: TaskEngineMessageToProcess, e: Exception) = logger.error(
    "taskId {} - exception on message {}:${System.getProperty("line.separator")}{}",
    messageToProcess.message.taskId,
    messageToProcess.message,
    e
)

fun <T : TaskEngineMessageToProcess> CoroutineScope.startTaskEngine(
    coroutineName: String,
    taskStateStorage: TaskStateStorage,
    taskCommandsChannel: ReceiveChannel<T>,
    taskEventsChannel: Channel<T>,
    logChannel: SendChannel<T>,
    sendToClient: SendToClient,
    sendToTagEngine: SendToTagEngine,
    sendToTaskEngine: SendToTaskEngine,
    sendToWorkflowEngine: SendToWorkflowEngine,
    sendToTaskExecutors: SendToTaskExecutors,
    sendToMetricsPerName: SendToMetricsPerName
) = launch(CoroutineName(coroutineName)) {

    val taskEngine = TaskEngine(
        taskStateStorage,
        sendToClient,
        sendToTagEngine,
        sendToTaskEngine,
        sendToWorkflowEngine,
        sendToTaskExecutors,
        sendToMetricsPerName
    )

    while (true) {
        select<Unit> {
            taskEventsChannel.onReceive {
                try {
                    it.returnValue = taskEngine.handle(it.message)
                } catch (e: Exception) {
                    it.exception = e
                    logError(it, e)
                } finally {
                    logChannel.send(it)
                }
            }
            taskCommandsChannel.onReceive {
                try {
                    it.returnValue = taskEngine.handle(it.message)
                } catch (e: Exception) {
                    it.exception = e
                    logError(it, e)
                } finally {
                    logChannel.send(it)
                }
            }
        }
    }
}
