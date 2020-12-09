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

package io.infinitic.pulsar.workers.taskEngine

import io.infinitic.common.serDe.kotlin.readBinary
import io.infinitic.common.storage.keyValue.KeyValueStorage
import io.infinitic.common.tasks.engine.messages.TaskEngineEnvelope
import io.infinitic.common.tasks.engine.messages.TaskEngineMessage
import io.infinitic.pulsar.schemas.schemaDefinition
import io.infinitic.pulsar.topics.TaskEngineCommandsTopic
import io.infinitic.pulsar.topics.TaskEngineEventsTopic
import io.infinitic.pulsar.transport.PulsarTaskEngineOutput
import io.infinitic.pulsar.workers.PulsarMessageToProcess
import io.infinitic.tasks.engine.storage.events.NoTaskEventStorage
import io.infinitic.tasks.engine.storage.states.TaskStateKeyValueStorage
import io.infinitic.tasks.engine.transport.TaskEngineInput
import io.infinitic.tasks.engine.worker.startTaskEngine
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

typealias PulsarTaskEngineMessageToProcess = PulsarMessageToProcess<TaskEngineMessage>

fun CoroutineScope.startEngineTaskWorker(
    pulsarClient: PulsarClient,
    keyValueStorage: KeyValueStorage,

) = launch(Dispatchers.IO) {

    val taskCommandsChannel = Channel<PulsarTaskEngineMessageToProcess>()
    val taskEventsChannel = Channel<PulsarTaskEngineMessageToProcess>()
    val taskResultsChannel = Channel<PulsarTaskEngineMessageToProcess>()

    // Starting Task Engine
    startTaskEngine(
        TaskStateKeyValueStorage(keyValueStorage),
        NoTaskEventStorage(),
        TaskEngineInput(
            taskCommandsChannel,
            taskEventsChannel,
            taskResultsChannel
        ),
        PulsarTaskEngineOutput.from(pulsarClient)
    )

    repeat(1) {
        // create task engine consumer
        val taskEngineConsumer: Consumer<TaskEngineEnvelope> =
            pulsarClient.newConsumer(Schema.AVRO(schemaDefinition<TaskEngineEnvelope>()))
                .topics(listOf(TaskEngineCommandsTopic.name, TaskEngineEventsTopic.name))
                .subscriptionName("task-engine-consumer") // FIXME: Should be in a constant somewhere
                .subscriptionType(SubscriptionType.Key_Shared)
                .subscribe()

        // coroutine dedicated to pulsar message acknowledging
        launch {
            for (messageToProcess in taskResultsChannel) {
                if (messageToProcess.exception == null) {
                    taskEngineConsumer.acknowledgeAsync(messageToProcess.messageId).await()
                } else {
                    taskEngineConsumer.negativeAcknowledge(messageToProcess.messageId)
                }
            }
        }

        // coroutine dedicated to pulsar message pulling
        launch {
            while (isActive) {
                val message: Message<TaskEngineEnvelope> = taskEngineConsumer.receiveAsync().await()

                try {
                    val envelope = readBinary(message.data, TaskEngineEnvelope.serializer())
                    taskCommandsChannel.send(PulsarMessageToProcess(envelope.message(), message.messageId))
                } catch (e: Exception) {
                    taskEngineConsumer.negativeAcknowledge(message)
                    throw e
                }
            }
        }
    }
}
