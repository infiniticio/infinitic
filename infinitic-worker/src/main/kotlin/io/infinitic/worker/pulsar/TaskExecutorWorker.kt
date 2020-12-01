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

package io.infinitic.worker.pulsar

import io.infinitic.common.storage.keyValue.KeyValueStorage
import io.infinitic.common.tasks.executors.messages.TaskExecutorMessage
import io.infinitic.pulsar.extensions.newTaskConsumer
import io.infinitic.pulsar.transport.PulsarTransport
import io.infinitic.tasks.engine.storage.TaskStateKeyValueStorage
import io.infinitic.tasks.executor.TaskExecutor
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.future.await
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.apache.pulsar.client.api.MessageId
import org.apache.pulsar.client.api.PulsarClient
import kotlin.coroutines.CoroutineContext

class TaskExecutorWorker(storage: KeyValueStorage) : CoroutineScope {

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.IO + Job()

    private val taskStateStorage = TaskStateKeyValueStorage(storage)
    private var pulsarClient: PulsarClient? = null

//    fun run() {
//        try {
//            start()
//        } catch (e: PulsarClientException) {
//            println(e) // FIXME: Remove and replace by a logger
//            stop()
//
//            throw e
//        }
//    }

    fun stop() {
        cancel()
        pulsarClient?.close()
    }

//    fun runWithoutConcurrency() {
//        taskExecutor.getRegisteredTasks().map { name ->
//            pulsarClient.newTaskConsumer(name)
//        }.forEach { consumer ->
//            startConsumer(consumer) { taskExecutor.handle(it.value.message()) }
//        }
//    }

    fun start(pulsarClient: PulsarClient) {
        this.pulsarClient = pulsarClient

        val transport = PulsarTransport.from(pulsarClient)

        val taskExecutor = TaskExecutor(transport.sendToTaskEngine)

        val workerInputChannel = Channel<MessageToProcess<TaskExecutorMessage>>()

        // launch 8 workers
        repeat(8) {
            launch(CoroutineName("worker-$it")) {
                for (work in workerInputChannel) {
                    taskExecutor.handle(work.message)
                    // TODO Handle unexpected error
                    work.replyTo.send(MessageProcessed(work.messageId))
                }
            }
        }

        taskExecutor.getRegisteredTasks().forEach { taskName ->
            val consumer = pulsarClient.newTaskConsumer(taskName)
            val resultChannel = Channel<MessageProcessed>(8)

            // launch 1 consumer
            launch(CoroutineName("consumer-$taskName")) {
                while (isActive) {
                    val envelope = consumer.receiveAsync().await()
                    workerInputChannel.send(
                        MessageToProcess(
                            envelope.messageId,
                            envelope.value.message(),
                            resultChannel
                        )
                    )
                }
            }

            // launch 1 acknowledger
            launch(CoroutineName("ack-$taskName")) {
                for (message in resultChannel) {
                    consumer.acknowledgeAsync(message.messageId).await()
                }
            }
        }
    }
}

data class MessageToProcess<T> (
    val messageId: MessageId,
    val message: T,
    val replyTo: SendChannel<MessageProcessed>
)

data class MessageProcessed(val messageId: MessageId)
