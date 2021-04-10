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

package io.infinitic.tags.engine.worker

import io.infinitic.common.clients.transport.SendToClient
import io.infinitic.common.tags.messages.TagEngineMessage
import io.infinitic.common.tasks.engine.transport.SendToTaskEngine
import io.infinitic.common.workers.MessageToProcess
import io.infinitic.common.workflows.engine.transport.SendToWorkflowEngine
import io.infinitic.tags.engine.TagEngine
import io.infinitic.tags.engine.storage.TagStateStorage
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.launch
import kotlinx.coroutines.selects.select
import org.slf4j.Logger
import org.slf4j.LoggerFactory

private val logger: Logger
    get() = LoggerFactory.getLogger(TagEngine::class.java)

typealias TagEngineMessageToProcess = MessageToProcess<TagEngineMessage>

private fun logError(messageToProcess: TagEngineMessageToProcess, e: Exception) = logger.error(
    "exception on message {}:${System.getProperty("line.separator")}{}",
    messageToProcess.message,
    e
)

fun <T : TagEngineMessageToProcess> CoroutineScope.startTagEngine(
    coroutineName: String,
    tagStateStorage: TagStateStorage,
    eventsInputChannel: ReceiveChannel<T>,
    eventsOutputChannel: SendChannel<T>,
    commandsInputChannel: ReceiveChannel<T>,
    commandsOutputChannel: SendChannel<T>,
    sendToClient: SendToClient,
    sendToTaskEngine: SendToTaskEngine,
    sendToWorkflowEngine: SendToWorkflowEngine
) = launch(CoroutineName(coroutineName)) {

    val tagEngine = TagEngine(
        tagStateStorage,
        sendToClient,
        sendToTaskEngine,
        sendToWorkflowEngine
    )

    while (true) {
        select<Unit> {
            eventsInputChannel.onReceive {
                try {
                    it.returnValue = tagEngine.handle(it.message)
                } catch (e: Exception) {
                    it.throwable = e
                    logError(it, e)
                } finally {
                    eventsOutputChannel.send(it)
                }
            }
            commandsInputChannel.onReceive {
                try {
                    it.returnValue = tagEngine.handle(it.message)
                } catch (e: Exception) {
                    it.throwable = e
                    logError(it, e)
                } finally {
                    commandsOutputChannel.send(it)
                }
            }
        }
    }
}
