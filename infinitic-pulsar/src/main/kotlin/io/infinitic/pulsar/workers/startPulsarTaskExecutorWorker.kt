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
import io.infinitic.common.tasks.executors.messages.TaskExecutorMessage
import io.infinitic.pulsar.transport.PulsarMessageToProcess
import io.infinitic.tasks.executor.register.TaskExecutorRegister
import io.infinitic.tasks.executor.transport.TaskExecutorInput
import io.infinitic.tasks.executor.transport.TaskExecutorMessageToProcess
import io.infinitic.tasks.executor.transport.TaskExecutorOutput
import io.infinitic.tasks.executor.worker.startTaskExecutor
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

typealias PulsarTaskExecutorMessageToProcess = PulsarMessageToProcess<TaskExecutorMessage>

const val TASK_EXECUTOR_PROCESSING_COROUTINE_NAME = "task-executor-processing"
const val TASK_EXECUTOR_ACKNOWLEDGING_COROUTINE_NAME = "task-executor-acknowledging"
const val TASK_EXECUTOR_PULLING_COROUTINE_NAME = "task-executor-pulling"

fun CoroutineScope.startPulsarTaskExecutorWorker(
    taskName: String,
    consumerCounter: Int,
    taskExecutorConsumer: Consumer<TaskExecutorMessage>,
    taskExecutorOutput: TaskExecutorOutput,
    taskExecutorRegister: TaskExecutorRegister,
    logChannel: SendChannel<TaskExecutorMessageToProcess>?,
    instancesNumber: Int = 1
) = launch(Dispatchers.IO) {

    val taskExecutorChannel = Channel<PulsarTaskExecutorMessageToProcess>()
    val taskExecutorResultsChannel = Channel<PulsarTaskExecutorMessageToProcess>()

    // start task executor
    repeat(instancesNumber) {
        startTaskExecutor(
            "$TASK_EXECUTOR_PROCESSING_COROUTINE_NAME-$taskName-$consumerCounter-$it",
            taskExecutorRegister,
            TaskExecutorInput(taskExecutorChannel, taskExecutorResultsChannel),
            taskExecutorOutput,
        )
    }

    // coroutine dedicated to pulsar message acknowledging
    launch(CoroutineName("$TASK_EXECUTOR_ACKNOWLEDGING_COROUTINE_NAME-$taskName-$consumerCounter")) {
        for (messageToProcess in taskExecutorResultsChannel) {
            if (messageToProcess.exception == null) {
                taskExecutorConsumer.acknowledgeAsync(messageToProcess.messageId).await()
            } else {
                taskExecutorConsumer.negativeAcknowledge(messageToProcess.messageId)
            }
            logChannel?.send(messageToProcess)
        }
    }

    // coroutine dedicated to pulsar message pulling
    launch(CoroutineName("$TASK_EXECUTOR_PULLING_COROUTINE_NAME-$taskName-$consumerCounter")) {
        while (isActive) {
            val message: Message<TaskExecutorMessage> = taskExecutorConsumer.receiveAsync().await()

            try {
                val taskExecutorMessage = readBinary(message.data, TaskExecutorMessage.serializer())
                taskExecutorChannel.send(PulsarMessageToProcess(taskExecutorMessage, message.messageId))
            } catch (e: Exception) {
                taskExecutorConsumer.negativeAcknowledge(message.messageId)
                throw e
            }
        }
    }
}
