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

import io.infinitic.common.storage.keyValue.KeyValueCache
import io.infinitic.common.storage.keyValue.KeyValueStorage
import io.infinitic.common.tasks.engine.messages.TaskEngineEnvelope
import io.infinitic.common.tasks.engine.messages.TaskEngineMessage
import io.infinitic.common.tasks.engine.state.TaskState
import io.infinitic.common.tasks.engine.transport.SendToTaskEngine
import io.infinitic.common.workers.singleThreadedContext
import io.infinitic.pulsar.InfiniticWorker
import io.infinitic.tasks.engine.TaskEngine
import io.infinitic.tasks.engine.storage.events.NoTaskEventStorage
import io.infinitic.tasks.engine.storage.states.TaskStateKeyValueStorage
import io.infinitic.tasks.engine.transport.TaskEngineOutput
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.future.await
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.apache.pulsar.client.api.Consumer
import org.apache.pulsar.client.api.Message
import org.apache.pulsar.client.api.MessageId
import org.slf4j.Logger
import org.slf4j.LoggerFactory

const val TASK_ENGINE_THREAD_NAME = "task-engine-processing"

private val logger: Logger
    get() = LoggerFactory.getLogger(InfiniticWorker::class.java)

private fun logError(message: Message<TaskEngineEnvelope>, e: Exception) = logger.error(
    "exception on Pulsar message {}:${System.getProperty("line.separator")}{}",
    message,
    e
)

private fun logError(message: TaskEngineMessage, e: Exception) = logger.error(
    "taskId {} - exception on message {}:${System.getProperty("line.separator")}{}",
    message.taskId,
    message,
    e
)

fun CoroutineScope.startPulsarTaskEngineWorker(
    consumerCounter: Int,
    taskEngineConsumer: Consumer<TaskEngineEnvelope>,
    taskEngineOutput: TaskEngineOutput,
    sendToTaskEngineDeadLetters: SendToTaskEngine,
    keyValueStorage: KeyValueStorage,
    keyValueCache: KeyValueCache<TaskState>
) = launch(singleThreadedContext("$TASK_ENGINE_THREAD_NAME-$consumerCounter")) {

    val taskEngine = TaskEngine(
        TaskStateKeyValueStorage(keyValueStorage, keyValueCache),
        NoTaskEventStorage(),
        taskEngineOutput
    )

    fun negativeAcknowledge(pulsarId: MessageId) =
        taskEngineConsumer.negativeAcknowledge(pulsarId)

    suspend fun acknowledge(pulsarId: MessageId) =
        taskEngineConsumer.acknowledgeAsync(pulsarId).await()

    while (isActive) {
        val pulsarMessage = taskEngineConsumer.receiveAsync().await()

        val message = try {
            TaskEngineEnvelope.fromByteArray(pulsarMessage.data).message()
        } catch (e: Exception) {
            logError(pulsarMessage, e)
            negativeAcknowledge(pulsarMessage.messageId)

            null
        }

        message?.let {
            try {
                taskEngine.handle(it)

                acknowledge(pulsarMessage.messageId)
            } catch (e: Exception) {
                logError(message, e)
                negativeAcknowledge(pulsarMessage.messageId)
            }
        }
    }
}
