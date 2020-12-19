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

import io.infinitic.common.serDe.kotlin.readBinary
import io.infinitic.common.storage.keyValue.KeyValueStorage
import io.infinitic.common.tasks.engine.messages.TaskEngineEnvelope
import io.infinitic.common.tasks.engine.messages.TaskEngineMessage
import io.infinitic.pulsar.transport.PulsarMessageToProcess
import io.infinitic.tasks.engine.storage.events.NoTaskEventStorage
import io.infinitic.tasks.engine.storage.states.TaskStateKeyValueStorage
import io.infinitic.tasks.engine.transport.TaskEngineInputChannels
import io.infinitic.tasks.engine.transport.TaskEngineMessageToProcess
import io.infinitic.tasks.engine.transport.TaskEngineOutput
import io.infinitic.tasks.engine.worker.startTaskEngine
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.future.await
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.apache.pulsar.client.api.Consumer
import org.apache.pulsar.client.api.Message

typealias PulsarTaskEngineMessageToProcess = PulsarMessageToProcess<TaskEngineMessage>

const val TASK_ENGINE_PROCESSING_COROUTINE_NAME = "task-engine-processing"
const val TASK_ENGINE_ACKNOWLEDGING_COROUTINE_NAME = "task-engine-acknowledging"
const val TASK_ENGINE_PULLING_COROUTINE_NAME = "task-engine-pulling"

fun CoroutineScope.startPulsarTaskEngineWorker(
    consumerCounter: Int,
    taskEngineConsumer: Consumer<TaskEngineEnvelope>,
    taskEngineOutput: TaskEngineOutput,
    keyValueStorage: KeyValueStorage,
    logChannel: SendChannel<TaskEngineMessageToProcess>?,
) = launch(Dispatchers.IO) {

    val taskCommandsChannel = Channel<PulsarTaskEngineMessageToProcess>()
    val taskEventsChannel = Channel<PulsarTaskEngineMessageToProcess>()
    val taskResultsChannel = Channel<PulsarTaskEngineMessageToProcess>()

    // Starting Task Engine
    startTaskEngine(
        "$TASK_ENGINE_PROCESSING_COROUTINE_NAME-$consumerCounter",
        TaskStateKeyValueStorage(keyValueStorage),
        NoTaskEventStorage(),
        TaskEngineInputChannels(taskCommandsChannel, taskEventsChannel, taskResultsChannel),
        taskEngineOutput
    )

    // coroutine dedicated to pulsar message acknowledging
    launch(CoroutineName("$TASK_ENGINE_ACKNOWLEDGING_COROUTINE_NAME-$consumerCounter")) {
        for (messageToProcess in taskResultsChannel) {
            if (messageToProcess.exception == null) {
                taskEngineConsumer.acknowledgeAsync(messageToProcess.messageId).await()
            } else {
                taskEngineConsumer.negativeAcknowledge(messageToProcess.messageId)
            }
            logChannel?.send(messageToProcess)
        }
    }

    // coroutine dedicated to pulsar message pulling
    launch(CoroutineName("$TASK_ENGINE_PULLING_COROUTINE_NAME-$consumerCounter")) {
        while (isActive) {
            val message: Message<TaskEngineEnvelope> = taskEngineConsumer.receiveAsync().await()

            try {
                val envelope = readBinary(message.data, TaskEngineEnvelope.serializer())
                taskCommandsChannel.send(PulsarMessageToProcess(envelope.message(), message.messageId))
            } catch (e: Exception) {
                taskEngineConsumer.negativeAcknowledge(message.messageId)
                throw e
            }
        }
    }
}
