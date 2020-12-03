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
import io.infinitic.common.tasks.engine.transport.SendToTaskEngine
import io.infinitic.common.workflows.engine.SendToWorkflowEngine
import io.infinitic.common.workflows.engine.messages.WorkflowEngineMessage
import io.infinitic.common.workflows.engine.storage.InsertWorkflowEvent
import io.infinitic.workflows.engine.WorkflowEngine
import io.infinitic.workflows.engine.storage.WorkflowStateStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.selects.select

fun CoroutineScope.startWorkflowEngine(
    workflowStateStorage: WorkflowStateStorage,
    workflowClientChannel: ReceiveChannel<WorkflowEngineMessage>,
    workflowResultChannel: Channel<WorkflowEngineMessage>,
    taskClientChannel: SendChannel<TaskEngineMessage>
) = launch {

    // TODO store task events
    val insertWorkflowEvent: InsertWorkflowEvent = { }

    val sendToWorkflowEngine: SendToWorkflowEngine = { msg: WorkflowEngineMessage, after: Float ->
        // As it's a back loop, we need to trigger it asynchronously to avoid deadlocks
        // TODO in memory resilience implies to find a way to persist delayed messages
        launch {
            delay((1000 * after).toLong())
            workflowResultChannel.send(msg)
        }
    }

    val sendToTaskEngine: SendToTaskEngine = { msg: TaskEngineMessage, _: Float ->
        taskClientChannel.send(msg)
    }

    val workflowEngine = WorkflowEngine(
        workflowStateStorage,
        insertWorkflowEvent,
        sendToWorkflowEngine,
        sendToTaskEngine
    )

    while (true) {
        select<Unit> {
            workflowResultChannel.onReceive { workflowEngine.handle(it) }
            workflowClientChannel.onReceive { workflowEngine.handle(it) }
        }
    }
}
