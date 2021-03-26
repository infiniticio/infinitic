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

import io.infinitic.tags.engine.TagEngine
import io.infinitic.tags.engine.storage.TagStateStorage
import io.infinitic.tags.engine.transport.TagEngineInputChannels
import io.infinitic.tags.engine.transport.TagEngineMessageToProcess
import io.infinitic.tags.engine.transport.TagEngineOutput
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.selects.select
import org.slf4j.Logger
import org.slf4j.LoggerFactory

private val logger: Logger
    get() = LoggerFactory.getLogger(TagEngine::class.java)

private fun logError(messageToProcess: TagEngineMessageToProcess, e: Exception) = logger.error(
    "tag {} - exception on message {}:${System.getProperty("line.separator")}{}",
    messageToProcess.message.tag,
    messageToProcess.message,
    e
)

fun <T : TagEngineMessageToProcess> CoroutineScope.startTagEngine(
    coroutineName: String,
    tagStateStorage: TagStateStorage,
    tagEngineInputChannels: TagEngineInputChannels<T>,
    tagEngineOutput: TagEngineOutput
) = launch(CoroutineName(coroutineName)) {

    val tagEngine = TagEngine(tagStateStorage, tagEngineOutput)

    val out = tagEngineInputChannels.logChannel
    val events = tagEngineInputChannels.tagEngineEventsChannel
    val commands = tagEngineInputChannels.tagEngineCommandsChannel

    while (true) {
        select<Unit> {
            events.onReceive {
                try {
                    it.returnValue = tagEngine.handle(it.message)
                } catch (e: Exception) {
                    it.exception = e
                    logError(it, e)
                } finally {
                    out.send(it)
                }
            }
            commands.onReceive {
                try {
                    it.returnValue = tagEngine.handle(it.message)
                } catch (e: Exception) {
                    it.exception = e
                    logError(it, e)
                } finally {
                    out.send(it)
                }
            }
        }
    }
}
