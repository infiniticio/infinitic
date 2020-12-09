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
import io.infinitic.pulsar.schemas.schemaDefinition
import io.infinitic.pulsar.topics.TaskExecutorTopic
import io.infinitic.pulsar.transport.PulsarTaskExecutorOutput
import io.infinitic.tasks.executor.transport.TaskExecutorInput
import io.infinitic.tasks.executor.worker.startTaskExecutor
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.future.await
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.apache.pulsar.client.api.Consumer
import org.apache.pulsar.client.api.Message
import org.apache.pulsar.client.api.PulsarClient
import org.apache.pulsar.client.api.Schema
import org.apache.pulsar.client.api.SubscriptionType

typealias PulsarTaskExecutorMessageToProcess = PulsarMessageToProcess<TaskExecutorMessage>

fun CoroutineScope.startExecutorWorker(
    pulsarClient: PulsarClient,
    name: String,
    instancesNumber: Int = 1
) = launch(Dispatchers.IO) {

    val taskExecutorChannel = Channel<PulsarTaskExecutorMessageToProcess>()
    val taskExecutorResultsChannel = Channel<PulsarTaskExecutorMessageToProcess>()

    // Starting Task Engine
    repeat(instancesNumber) {
        startTaskExecutor(
            "task-executor-$it",
            TaskExecutorInput(taskExecutorChannel, taskExecutorResultsChannel),
            PulsarTaskExecutorOutput.from(pulsarClient)
        )
    }

    // create task executor consumer
    val taskEngineConsumer: Consumer<TaskExecutorEnvelope> =
        pulsarClient.newConsumer(Schema.AVRO(schemaDefinition<TaskExecutorEnvelope>()))
            .topics(listOf(TaskExecutorTopic.name(name)))
            .subscriptionName("task-executor-consumer-$name")
            .subscriptionType(SubscriptionType.Shared)
            .subscribe()

    // coroutine dedicated to pulsar message acknowledging
    launch(CoroutineName("task-engine-message-acknowledger")) {
        for (messageToProcess in taskExecutorResultsChannel) {
            if (messageToProcess.exception == null) {
                taskEngineConsumer.acknowledgeAsync(messageToProcess.messageId).await()
            } else {
                taskEngineConsumer.negativeAcknowledge(messageToProcess.messageId)
            }
        }
    }

    // coroutine dedicated to pulsar message pulling
    launch(CoroutineName("task-engine-message-puller")) {
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
