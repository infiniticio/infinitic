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

import io.infinitic.common.tasks.engine.transport.SendToTaskEngine
import io.infinitic.common.tasks.executors.messages.TaskExecutorEnvelope
import io.infinitic.common.tasks.executors.messages.TaskExecutorMessage
import io.infinitic.pulsar.InfiniticWorker
import io.infinitic.pulsar.transport.PulsarMessageToProcess
import io.infinitic.tasks.TaskExecutorRegister
import io.infinitic.tasks.executor.worker.startTaskExecutor
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.future.await
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.apache.pulsar.client.api.Consumer
import org.apache.pulsar.client.api.Message
import org.apache.pulsar.client.api.MessageId
import org.slf4j.Logger
import org.slf4j.LoggerFactory

typealias PulsarTaskExecutorMessageToProcess = PulsarMessageToProcess<TaskExecutorMessage>

const val TASK_EXECUTOR_PROCESSING_COROUTINE_NAME = "task-executor-processing"
const val TASK_EXECUTOR_ACKNOWLEDGING_COROUTINE_NAME = "task-executor-acknowledging"
const val TASK_EXECUTOR_PULLING_COROUTINE_NAME = "task-executor-pulling"

private val logger: Logger
    get() = LoggerFactory.getLogger(InfiniticWorker::class.java)

private fun logError(message: Message<TaskExecutorEnvelope>, e: Exception) = logger.error(
    "exception on message {}:${System.getProperty("line.separator")}{}",
    message,
    e
)

fun CoroutineScope.startPulsarTaskExecutorWorker(
    dispatcher: CoroutineDispatcher,
    taskName: String,
    consumerCounter: Int,
    taskExecutorConsumer: Consumer<TaskExecutorEnvelope>,
    sendToTaskEngine: SendToTaskEngine,
    taskExecutorRegister: TaskExecutorRegister,
    instancesNumber: Int = 1
) = launch(dispatcher) {

    val taskExecutorChannel = Channel<PulsarTaskExecutorMessageToProcess>()
    val taskExecutorResultsChannel = Channel<PulsarTaskExecutorMessageToProcess>()

    // start task executor
    repeat(instancesNumber) {
        startTaskExecutor(
            "$TASK_EXECUTOR_PROCESSING_COROUTINE_NAME-$taskName-$consumerCounter-$it",
            taskExecutorRegister,
            taskExecutorChannel,
            taskExecutorResultsChannel,
            sendToTaskEngine
        )
    }

    fun negativeAcknowledge(pulsarId: MessageId) =
        taskExecutorConsumer.negativeAcknowledge(pulsarId)

    suspend fun acknowledge(pulsarId: MessageId) =
        taskExecutorConsumer.acknowledgeAsync(pulsarId).await()

    // coroutine dedicated to pulsar message acknowledging
    launch(CoroutineName("$TASK_EXECUTOR_ACKNOWLEDGING_COROUTINE_NAME-$taskName-$consumerCounter")) {
        for (messageToProcess in taskExecutorResultsChannel) {
            when (messageToProcess.exception) {
                null -> acknowledge(messageToProcess.pulsarId)
                else -> negativeAcknowledge(messageToProcess.pulsarId)
            }
        }
    }

    // coroutine dedicated to pulsar message pulling
    launch(CoroutineName("$TASK_EXECUTOR_PULLING_COROUTINE_NAME-$taskName-$consumerCounter")) {
        while (isActive) {
            val message: Message<TaskExecutorEnvelope> = taskExecutorConsumer.receiveAsync().await()

            try {
                val taskExecutorMessage = TaskExecutorEnvelope.fromByteArray(message.data).message()
                taskExecutorChannel.send(
                    PulsarMessageToProcess(
                        message = taskExecutorMessage,
                        pulsarId = message.messageId,
                        redeliveryCount = message.redeliveryCount
                    )
                )
            } catch (e: Exception) {
                logError(message, e)
                negativeAcknowledge(message.messageId)
            }
        }
    }
}
