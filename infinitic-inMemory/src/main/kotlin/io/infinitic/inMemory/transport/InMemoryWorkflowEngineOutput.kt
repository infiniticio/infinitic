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

package io.infinitic.inMemory.transport

import io.infinitic.common.clients.messages.ClientResponseMessage
import io.infinitic.common.clients.transport.ClientResponseMessageToProcess
import io.infinitic.common.clients.transport.SendToClientResponse
import io.infinitic.common.data.MillisDuration
import io.infinitic.common.tasks.engine.messages.TaskEngineMessage
import io.infinitic.common.tasks.engine.transport.SendToTaskEngine
import io.infinitic.common.workflows.engine.messages.WorkflowEngineMessage
import io.infinitic.common.workflows.engine.transport.SendToWorkflowEngine
import io.infinitic.tasks.engine.transport.TaskEngineMessageToProcess
import io.infinitic.workflows.engine.transport.WorkflowEngineMessageToProcess
import io.infinitic.workflows.engine.transport.WorkflowEngineOutput
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class InMemoryWorkflowEngineOutput(
    scope: CoroutineScope,
    clientResponseChannel: Channel<ClientResponseMessageToProcess>,
    taskCommandsChannel: Channel<TaskEngineMessageToProcess>,
    workflowEventsChannel: SendChannel<WorkflowEngineMessageToProcess>
) : WorkflowEngineOutput {

    override val sendToClientResponseFn: SendToClientResponse = { msg: ClientResponseMessage ->
        // As it's a back loop, we trigger it asynchronously to avoid deadlocks
        scope.launch {
            clientResponseChannel.send(InMemoryMessageToProcess(msg))
        }
    }

    override val sendToWorkflowEngineFn: SendToWorkflowEngine = { msg: WorkflowEngineMessage, after: MillisDuration ->
        // As it's a back loop, we trigger it asynchronously to avoid deadlocks
        scope.launch {
            delay(after.long)
            workflowEventsChannel.send(InMemoryMessageToProcess(msg))
        }
    }

    override val sendToTaskEngineFn: SendToTaskEngine = { msg: TaskEngineMessage, after: MillisDuration ->
        // As it's a back loop, we trigger it asynchronously to avoid deadlocks
        scope.launch {
            delay(after.long)
            taskCommandsChannel.send(InMemoryMessageToProcess(msg))
        }
    }
}
