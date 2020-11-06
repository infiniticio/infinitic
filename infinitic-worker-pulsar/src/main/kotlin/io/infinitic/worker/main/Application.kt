// "Commons Clause" License Condition v1.0
//
// The Software is provided to you by the Licensor under the License, as defined
// below, subject to the following condition.
//
// Without limiting other conditions in the License, the grant of rights under the
// License will not include, and the License does not grant to you, the right to
// Sell the Software.
//
// For purposes of the foregoing, “Sell” means practicing any or all of the rights
// granted to you under the License to provide to third parties, for a fee or
// other consideration (including without limitation fees for hosting or
// consulting/ support services related to the Software), a product or service
// whose value derives, entirely or substantially, from the functionality of the
// Software. Any license notice or attribution required by the License must also
// include this Commons Clause License Condition notice.
//
// Software: Infinitic
//
// License: MIT License (https://opensource.org/licenses/MIT)
//
// Licensor: infinitic.io

package io.infinitic.worker.main

import io.infinitic.common.serDe.avro.AvroConverter
import io.infinitic.common.tasks.messages.workerMessages.WorkerMessage
import io.infinitic.messaging.api.dispatcher.Dispatcher
import io.infinitic.worker.Worker
import io.infinitic.worker.extensions.acknowledgeSuspend
import io.infinitic.worker.extensions.newTaskConsumer
import io.infinitic.worker.extensions.receiveSuspend
import io.infinitic.worker.extensions.startConsumer
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.apache.pulsar.client.api.PulsarClient
import org.apache.pulsar.client.api.PulsarClientException
import kotlin.concurrent.thread
import kotlin.coroutines.CoroutineContext

class Application internal constructor(private val pulsarClient: PulsarClient, private val worker: Worker) : CoroutineScope {
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.IO + Job()

    init {
        Runtime.getRuntime().addShutdownHook(
            thread(start = false) {
                println("Stopping because of the shutdown hook.")
                stop()
            }
        )
    }

    fun run() {
        try {
            runWithConcurrency()
        } catch (e: PulsarClientException) {
            println(e) // FIXME: Remove and replace by a logger
            stop()

            throw e
        }
    }

    fun runWithoutConcurrency() {
        worker.getRegisteredTasks().map { name ->
            pulsarClient.newTaskConsumer(name)
        }.forEach { consumer ->
            startConsumer(consumer) { message ->
                message
                    .let { AvroConverter.fromWorkers(it.value) }
                    .let { worker.handle(it) }
            }
        }
    }

    fun runWithConcurrency() {
        val workerInputChannel = Channel<MessageToProcess<WorkerMessage>>()

        // launch 8 workers
        repeat(8) {
            launch(CoroutineName("worker-$it")) {
                for (work in workerInputChannel) {
                    worker.handle(work.message)
                    work.replyTo.send(MessageProcessed(work.messageId))
                }
            }
        }

        worker.getRegisteredTasks().map { name ->
            Pair(name, pulsarClient.newTaskConsumer(name))
        }.forEach { (taskName, consumer) ->
            val resultChannel = Channel<MessageProcessed>(8)

            // launch 1 consumer
            launch(CoroutineName("consumer-$taskName")) {
                while (isActive) {
                    val message = consumer.receiveSuspend()
                    message
                        .let { AvroConverter.fromWorkers(it.value) }
                        .let { workerInputChannel.send(MessageToProcess(message.messageId, it, resultChannel)) }
                }
            }

            // launch 1 acknowledger
            launch(CoroutineName("ack-$taskName")) {
                for (message in resultChannel) {
                    consumer.acknowledgeSuspend(message.messageId)
                }
            }
        }
    }

    fun stop() {
        cancel()
        pulsarClient.close()
    }
}

fun worker(pulsarClient: PulsarClient, dispatcher: Dispatcher, block: Worker.() -> Unit): Application {
    val worker = Worker(dispatcher)
    worker.block()

    return Application(pulsarClient, worker)
}
