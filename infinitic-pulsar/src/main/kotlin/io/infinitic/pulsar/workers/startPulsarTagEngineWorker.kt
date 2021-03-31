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

import io.infinitic.common.storage.keySet.KeySetStorage
import io.infinitic.common.storage.keyValue.KeyValueStorage
import io.infinitic.common.tags.messages.TagEngineEnvelope
import io.infinitic.common.tags.messages.TagEngineMessage
import io.infinitic.common.tags.transport.SendToTagEngine
import io.infinitic.common.workers.singleThreadedContext
import io.infinitic.pulsar.InfiniticWorker
import io.infinitic.tags.engine.TagEngine
import io.infinitic.tags.engine.output.TagEngineOutput
import io.infinitic.tags.engine.storage.BinaryTagStateStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.future.await
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.apache.pulsar.client.api.Consumer
import org.apache.pulsar.client.api.Message
import org.apache.pulsar.client.api.MessageId
import org.slf4j.Logger
import org.slf4j.LoggerFactory

const val TAG_ENGINE_THREAD_NAME = "tag-engine-processing"

private val logger: Logger
    get() = LoggerFactory.getLogger(InfiniticWorker::class.java)

private fun logError(message: Message<TagEngineEnvelope>, e: Exception) = logger.error(
    "exception on Pulsar message {}:${System.getProperty("line.separator")}{}",
    message,
    e
)

private fun logError(message: TagEngineMessage, e: Exception) = logger.error(
    "tag {} - exception on message {}:${System.getProperty("line.separator")}{}",
    message.tag,
    message,
    e
)

fun CoroutineScope.startPulsarTagEngineWorker(
    consumerCounter: Int,
    tagEngineConsumer: Consumer<TagEngineEnvelope>,
    tagEngineOutput: TagEngineOutput,
    sendToTagEngineDeadLetters: SendToTagEngine,
    keyValueStorage: KeyValueStorage,
    keySetStorage: KeySetStorage,
) = launch(singleThreadedContext("$TAG_ENGINE_THREAD_NAME-$consumerCounter")) {

    val tagEngine = TagEngine(
        BinaryTagStateStorage(keyValueStorage, keySetStorage),
        tagEngineOutput
    )

    fun negativeAcknowledge(pulsarId: MessageId) =
        tagEngineConsumer.negativeAcknowledge(pulsarId)

    suspend fun acknowledge(pulsarId: MessageId) =
        tagEngineConsumer.acknowledgeAsync(pulsarId).await()

    while (isActive) {
        val pulsarMessage = tagEngineConsumer.receiveAsync().await()

        val message = try {
            TagEngineEnvelope.fromByteArray(pulsarMessage.data).message()
        } catch (e: Exception) {
            logError(pulsarMessage, e)
            negativeAcknowledge(pulsarMessage.messageId)

            null
        }

        message?.let {
            try {
                tagEngine.handle(it)

                acknowledge(pulsarMessage.messageId)
            } catch (e: Exception) {
                logError(message, e)
                negativeAcknowledge(pulsarMessage.messageId)
            }
        }
    }
}
