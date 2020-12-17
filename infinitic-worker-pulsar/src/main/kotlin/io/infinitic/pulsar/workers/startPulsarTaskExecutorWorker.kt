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
import io.infinitic.common.tasks.executors.messages.TaskExecutorEnvelope
import io.infinitic.common.tasks.executors.messages.TaskExecutorMessage
import io.infinitic.pulsar.consumers.ConsumerFactory
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

fun CoroutineScope.startPulsarTaskExecutorWorker(
    consumerFactory: ConsumerFactory,
    taskExecutorOutput: TaskExecutorOutput,
    taskExecutorRegister: TaskExecutorRegister,
    logChannel: SendChannel<TaskExecutorMessageToProcess>?,
    instancesNumber: Int = 1
) = launch(Dispatchers.IO) {

    // for each task
    taskExecutorRegister.getTasks().forEach { name ->

        val taskExecutorChannel = Channel<PulsarTaskExecutorMessageToProcess>()
        val taskExecutorResultsChannel = Channel<PulsarTaskExecutorMessageToProcess>()

        // start task executor
        repeat(instancesNumber) {
            startTaskExecutor(
                taskExecutorRegister,
                TaskExecutorInput(taskExecutorChannel, taskExecutorResultsChannel),
                taskExecutorOutput,
                "-$name-$it"
            )
        }

        // create task executor consumer
        val taskEngineConsumer: Consumer<TaskExecutorEnvelope> = consumerFactory
            .newExecutorTaskConsumer(name)

        // coroutine dedicated to pulsar message acknowledging
        launch(CoroutineName("task-engine-message-acknowledger-$name")) {
            for (messageToProcess in taskExecutorResultsChannel) {
                if (messageToProcess.exception == null) {
                    taskEngineConsumer.acknowledgeAsync(messageToProcess.messageId).await()
                } else {
                    taskEngineConsumer.negativeAcknowledge(messageToProcess.messageId)
                }
                logChannel?.send(messageToProcess)
            }
        }

        // coroutine dedicated to pulsar message pulling
        launch(CoroutineName("task-engine-message-puller-$name")) {
            while (isActive) {
                val message: Message<TaskExecutorEnvelope> = taskEngineConsumer.receiveAsync().await()

                try {
                    val envelope = readBinary(message.data, TaskExecutorEnvelope.serializer())
                    taskExecutorChannel.send(PulsarMessageToProcess(envelope.message(), message.messageId))
                } catch (e: Exception) {
                    taskEngineConsumer.negativeAcknowledge(message.messageId)
                    throw e
                }
            }
        }
    }
}
