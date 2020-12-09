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

package io.infinitic.tasks.engine.worker

import io.infinitic.tasks.engine.TaskEngine
import io.infinitic.tasks.engine.storage.events.TaskEventStorage
import io.infinitic.tasks.engine.storage.states.TaskStateStorage
import io.infinitic.tasks.engine.transport.TaskEngineInput
import io.infinitic.tasks.engine.transport.TaskEngineMessageToProcess
import io.infinitic.tasks.engine.transport.TaskEngineOutput
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.selects.select

fun <T : TaskEngineMessageToProcess> CoroutineScope.startTaskEngine(
    coroutineName: String,
    taskStateStorage: TaskStateStorage,
    taskEventStorage: TaskEventStorage,
    taskEngineInput: TaskEngineInput<T>,
    taskEngineOutput: TaskEngineOutput
) = launch(CoroutineName(coroutineName)) {

    val taskEngine = TaskEngine(
        taskStateStorage,
        taskEventStorage,
        taskEngineOutput
    )

    val out = taskEngineInput.taskResultsChannel
    val events = taskEngineInput.taskEventsChannel
    val commands = taskEngineInput.taskCommandsChannel

    while (true) {
        select<Unit> {
            events.onReceive {
                try {
                    it.output = taskEngine.handle(it.message)
                    out.send(it)
                } catch (e: Exception) {
                    it.exception = e
                    out.send(it)
                }
            }
            commands.onReceive {
                try {
                    it.output = taskEngine.handle(it.message)
                    out.send(it)
                } catch (e: Exception) {
                    it.exception = e
                    out.send(it)
                }
            }
        }
    }
}
