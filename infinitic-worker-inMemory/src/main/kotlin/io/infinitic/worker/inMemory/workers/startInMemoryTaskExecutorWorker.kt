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

package io.infinitic.worker.inMemory

import io.infinitic.tasks.engine.transport.TaskEngineMessageToProcess
import io.infinitic.tasks.executor.register.TaskExecutorRegister
import io.infinitic.tasks.executor.transport.TaskExecutorInput
import io.infinitic.tasks.executor.transport.TaskExecutorMessageToProcess
import io.infinitic.tasks.executor.worker.startTaskExecutor
import io.infinitic.worker.inMemory.transport.InMemoryTaskExecutorOutput
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch

fun CoroutineScope.startInMemoryTaskExecutorWorker(
    taskExecutorRegister: TaskExecutorRegister,
    taskEngineEventsChannel: Channel<TaskEngineMessageToProcess>,
    taskExecutorChannel: Channel<TaskExecutorMessageToProcess>,
    taskExecutorResultsChannel: Channel<TaskExecutorMessageToProcess>,
    instancesNumber: Int = 1
) = launch {

    launch(CoroutineName("task-executor-message-acknowledger")) {
        for (result in taskExecutorResultsChannel) {
            println("TASK_EXECUTOR: ${result.message}")
            // no message acknowledging for inMemory implementation
            if (result.exception != null) {
                println(result.exception)
            }
        }
    }

    repeat(instancesNumber) {
        startTaskExecutor(
            taskExecutorRegister,
            TaskExecutorInput(taskExecutorChannel, taskExecutorResultsChannel),
            InMemoryTaskExecutorOutput(this, taskEngineEventsChannel),
            "-$it"
        )
    }
}
