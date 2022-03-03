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

package io.infinitic.transport.inMemory

import io.infinitic.common.data.MillisDuration
import io.infinitic.common.tasks.executors.messages.TaskExecutorMessage
import io.infinitic.tasks.executor.TaskExecutor
import io.infinitic.tasks.executor.worker.TaskExecutorMessageToProcess
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import mu.KotlinLogging

class InMemoryTaskExecutorTransport {
    private val logger = KotlinLogging.logger {}

    private val channel = Channel<TaskExecutorMessageToProcess>()

    fun CoroutineScope.send(message: TaskExecutorMessage, after: MillisDuration): Job {
        logger.debug { "after $after send $message " }

        return launch {
            delay(after.long)
            channel.send(InMemoryMessageToProcess(message))
        }
    }

    fun CoroutineScope.start(taskExecutor: TaskExecutor, concurrency: Int) {
        repeat(concurrency) {
            launch {
                for (messageToProcess in channel) {
                    try {
                        messageToProcess.returnValue = taskExecutor.handle(messageToProcess.message)
                    } catch (e: Throwable) {
                        messageToProcess.throwable = e
                        logger.error(e) { "exception on message ${messageToProcess.message}" }
                    }
                }
            }
        }
    }
}
