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

import io.infinitic.common.messages.Envelope
import io.infinitic.pulsar.InfiniticWorker
import io.infinitic.pulsar.transport.PulsarMessageToProcess
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.future.await
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.apache.pulsar.client.api.Consumer
import org.apache.pulsar.client.api.Message
import org.slf4j.Logger
import org.slf4j.LoggerFactory

private val logger: Logger
    get() = LoggerFactory.getLogger(InfiniticWorker::class.java)

private fun <S> logError(message: Message<S>, e: Throwable) = logger.error(
    "exception on Pulsar message {}: {}",
    message,
    e
)

fun <E : Envelope<M>, M> CoroutineScope.pullMessages(
    consumer: Consumer<E>,
    channel: SendChannel<PulsarMessageToProcess<M>>
) = launch {
    while (isActive) {
        val pulsarMessage = consumer.receiveAsync().await()

        val message = try {
            pulsarMessage.value.message()
        } catch (e: Throwable) {
            logError(pulsarMessage, e)
            consumer.negativeAcknowledge(pulsarMessage.messageId)

            null
        }

        message?.let {
            channel.send(
                PulsarMessageToProcess(
                    message = it,
                    pulsarId = pulsarMessage.messageId,
                    redeliveryCount = pulsarMessage.redeliveryCount
                )
            )
        }
    }
}

fun <E : Envelope<M>, M> CoroutineScope.acknowledgeMessages(
    consumer: Consumer<E>,
    channel: ReceiveChannel<PulsarMessageToProcess<M>>
) = launch {
    for (messageToProcess in channel) {
        when (messageToProcess.throwable) {
            null -> consumer.acknowledgeAsync(messageToProcess.pulsarId).await()
            else -> consumer.negativeAcknowledge(messageToProcess.pulsarId)
        }
    }
}
