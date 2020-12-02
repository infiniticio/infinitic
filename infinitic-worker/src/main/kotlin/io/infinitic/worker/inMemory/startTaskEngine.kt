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

import io.infinitic.common.tasks.engine.messages.TaskEngineMessage
import io.infinitic.common.tasks.executors.SendToExecutors
import io.infinitic.common.tasks.executors.messages.TaskExecutorMessage
import io.infinitic.tasks.engine.storage.TaskStateStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.launch

private fun CoroutineScope.startTaskEngine(
    taskStateStorage: TaskStateStorage,
    dispatchChannel: ReceiveChannel<TaskEngineMessage>,
    resultChannel: ReceiveChannel<TaskEngineMessage>,
    workerChannel: SendChannel<TaskExecutorMessage>,
) = launch {

    val sendToExecutors: SendToExecutors = { msg: TaskExecutorMessage -> workerChannel.send(msg) }

//    val taskEngine = TaskEngine(
//        taskStateStorage
//    )

//    while (true) {
//        select<Unit> {
//            resultChannel.onReceive { taskEngineMessage ->
//                when (taskEngineMessage) {
//                    is TaskCompleted -> {
//                        requested.remove(taskEngineMessage.taskId)
//                    }
//                    is TaskFailed -> {
//                        workerChannel.send(RunTask(taskEngineMessage.taskId))
//                    }
//                }
//            }
//            dispatchChannel.onReceive { taskEngineMessage ->
//                when (taskEngineMessage) {
//                    is DispatchTask -> {
//                        requested[taskEngineMessage.taskId] = true
//                        workerChannel.send(RunTask(taskEngineMessage.taskId))
//                    }
//                }
//            }
//        }
//    }
}
