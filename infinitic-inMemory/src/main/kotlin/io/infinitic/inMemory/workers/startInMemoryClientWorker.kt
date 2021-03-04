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

package io.infinitic.inMemory.workers

import io.infinitic.client.Client
import io.infinitic.common.clients.transport.ClientResponseMessageToProcess
import io.infinitic.monitoring.global.engine.MonitoringGlobalEngine
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.launch
import org.slf4j.Logger
import org.slf4j.LoggerFactory

private val logger: Logger
    get() = LoggerFactory.getLogger(MonitoringGlobalEngine::class.java)

private fun logError(messageToProcess: ClientResponseMessageToProcess, e: Exception) = logger.error(
    "exception on message {}:${System.getProperty("line.separator")}{}",
    messageToProcess.message,
    e
)

fun CoroutineScope.startInMemoryClientWorker(
    client: Client,
    clientChannel: Channel<ClientResponseMessageToProcess>,
    logChannel: SendChannel<ClientResponseMessageToProcess>
) = launch(CoroutineName("in-memory-client")) {

    for (message in clientChannel) {
        try {
            client.handle(message.message)
        } catch (e: Exception) {
            message.exception = e
            logError(message, e)
        } finally {
            logChannel.send(message)
        }
    }
}
