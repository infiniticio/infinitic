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

import io.infinitic.common.clients.transport.SendToClient
import io.infinitic.common.metrics.perName.transport.SendToMetricsPerName
import io.infinitic.common.storage.keyValue.KeyValueStorage
import io.infinitic.common.tags.transport.SendToTagEngine
import io.infinitic.common.tasks.engine.messages.TaskEngineEnvelope
import io.infinitic.common.tasks.engine.messages.TaskEngineMessage
import io.infinitic.common.tasks.engine.transport.SendToTaskEngine
import io.infinitic.common.tasks.executors.SendToTaskExecutors
import io.infinitic.common.workers.singleThreadedContext
import io.infinitic.common.workflows.engine.transport.SendToWorkflowEngine
import io.infinitic.pulsar.InfiniticWorker
import io.infinitic.pulsar.transport.PulsarMessageToProcess
import io.infinitic.tasks.engine.TaskEngine
import io.infinitic.tasks.engine.storage.BinaryTaskStateStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.future.await
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.selects.select
import kotlinx.coroutines.yield
import org.apache.pulsar.client.api.Consumer
import org.apache.pulsar.client.api.Message
import org.slf4j.Logger
import org.slf4j.LoggerFactory

const val TASK_ENGINE_THREAD_NAME = "task-engine"

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
    commandsConsumer: Consumer<TaskEngineEnvelope>,
    eventsConsumer: Consumer<TaskEngineEnvelope>,
    keyValueStorage: KeyValueStorage,
    sendToClient: SendToClient,
    sendToTagEngine: SendToTagEngine,
    sendToTaskEngine: SendToTaskEngine,
    sendToWorkflowEngine: SendToWorkflowEngine,
    sendToTaskExecutors: SendToTaskExecutors,
    sendToMetricsPerName: SendToMetricsPerName
) = launch(singleThreadedContext("$TASK_ENGINE_THREAD_NAME-$consumerCounter")) {

    val taskEngine = TaskEngine(
        BinaryTaskStateStorage(keyValueStorage),
        sendToClient,
        sendToTagEngine,
        sendToTaskEngine,
        sendToWorkflowEngine,
        sendToTaskExecutors,
        sendToMetricsPerName
    )

    val eventChannel = Channel<PulsarMessageToProcess<TaskEngineMessage>>()
    val commandChannel = Channel<PulsarMessageToProcess<TaskEngineMessage>>()

    suspend fun pullConsumerSendToChannel(
        consumer: Consumer<TaskEngineEnvelope>,
        channel: Channel<PulsarMessageToProcess<TaskEngineMessage>>
    ) {
        val pulsarMessage = consumer.receiveAsync().await()
        val message = try {
            TaskEngineEnvelope.fromByteArray(pulsarMessage.data).message()
        } catch (e: Exception) {
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
        yield()
    }

    suspend fun runEngine(
        messageToProcess: PulsarMessageToProcess<TaskEngineMessage>,
        consumer: Consumer<TaskEngineEnvelope>
    ) {
        try {
            taskEngine.handle(messageToProcess.message)
            consumer.acknowledge(messageToProcess.pulsarId)
        } catch (e: Exception) {
            logError(messageToProcess.message, e)
            consumer.negativeAcknowledge(messageToProcess.pulsarId)
        }
    }

    // Key-shared consumer for events messages
    launch {
        while (isActive) {
            pullConsumerSendToChannel(eventsConsumer, eventChannel)
        }
    }
    // Key-shared consumer for commands messages
    launch {
        while (isActive) {
            pullConsumerSendToChannel(commandsConsumer, commandChannel)
        }
    }

    // This implementation gives a priority to messages coming from events
    while (isActive) {
        select<Unit> {
            eventChannel.onReceive {
                runEngine(it, eventsConsumer)
            }
            commandChannel.onReceive {
                runEngine(it, commandsConsumer)
            }
        }
    }
}
