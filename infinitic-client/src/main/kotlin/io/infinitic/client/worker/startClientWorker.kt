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

package io.infinitic.client.worker

import io.infinitic.client.AbstractInfiniticClient
import io.infinitic.common.clients.InfiniticClient
import io.infinitic.common.clients.transport.ClientMessageToProcess
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.launch
import mu.KotlinLogging

private val logger = KotlinLogging.logger(AbstractInfiniticClient::class.java.name)

private fun logError(messageToProcess: ClientMessageToProcess, e: Exception) = logger.error(e) {
    "exception on message ${messageToProcess.message}: $e"
}

fun <T : ClientMessageToProcess> CoroutineScope.startClientWorker(
    client: InfiniticClient,
    inputChannel: ReceiveChannel<T>,
    outputChannel: SendChannel<T>
) = launch(CoroutineName("client: ${client.name}")) {

    for (message in inputChannel) {
        try {
            client.handle(message.message)
        } catch (e: Exception) {
            message.throwable = e
            logError(message, e)
        } finally {
            outputChannel.send(message)
        }
    }
}
