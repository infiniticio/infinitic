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

import io.infinitic.client.Client
import io.infinitic.common.clients.messages.ClientResponseEnvelope
import io.infinitic.common.clients.messages.ClientResponseMessage
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.future.await
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.apache.pulsar.client.api.Consumer
import org.apache.pulsar.client.api.Message
import org.apache.pulsar.client.api.MessageId
import org.apache.pulsar.client.api.PulsarClientException
import org.slf4j.Logger
import org.slf4j.LoggerFactory

const val CLIENT_RESPONSE_THREAD_NAME = "client-response"

private val logger: Logger
    get() = LoggerFactory.getLogger(io.infinitic.pulsar.InfiniticClient::class.java)

private fun logError(message: Message<ClientResponseEnvelope>, e: Exception) = logger.error(
    "exception on message {}:${System.getProperty("line.separator")}{}",
    message,
    e
)

private fun logError(message: ClientResponseMessage, e: Exception) = logger.error(
    "client {} - exception on message {}:${System.getProperty("line.separator")}{}",
    message.clientName,
    message,
    e
)

fun CoroutineScope.startClientResponseWorker(
    client: Client,
    clientResponseConsumer: Consumer<ClientResponseEnvelope>
) = launch(CoroutineName(CLIENT_RESPONSE_THREAD_NAME)) {

    fun negativeAcknowledge(pulsarId: MessageId) =
        clientResponseConsumer.negativeAcknowledge(pulsarId)

    suspend fun acknowledge(pulsarId: MessageId) =
        clientResponseConsumer.acknowledgeAsync(pulsarId).await()

    while (isActive) {
        try {
            val pulsarMessage = clientResponseConsumer.receiveAsync().await()

            val message = try {
                ClientResponseEnvelope.fromByteArray(pulsarMessage.data).message()
            } catch (e: Exception) {
                logError(pulsarMessage, e)
                negativeAcknowledge(pulsarMessage.messageId)

                null
            }

            message?.let {
                try {
                    client.handle(it)

                    acknowledge(pulsarMessage.messageId)
                } catch (e: Exception) {
                    logError(message, e)
                    negativeAcknowledge(pulsarMessage.messageId)
                }
            }
        } catch (e: PulsarClientException.AlreadyClosedException) {
            // if the pulsar client was closed => quit
            cancel()
        }
    }
}
