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

import io.infinitic.client.transport.ClientOutput
import io.infinitic.common.clients.data.ClientName
import io.infinitic.common.data.MillisDuration
import io.infinitic.common.tags.messages.TagEngineMessage
import io.infinitic.common.tags.transport.SendToTagEngine
import io.infinitic.common.tasks.engine.messages.TaskEngineMessage
import io.infinitic.common.tasks.engine.transport.SendToTaskEngine
import io.infinitic.common.workflows.engine.messages.WorkflowEngineMessage
import io.infinitic.common.workflows.engine.transport.SendToWorkflowEngine
import io.infinitic.tags.engine.transport.TagEngineMessageToProcess
import io.infinitic.tasks.engine.transport.TaskEngineMessageToProcess
import io.infinitic.workflows.engine.transport.WorkflowEngineMessageToProcess
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class InMemoryClientOutput(
    tagCommandsChannel: SendChannel<TagEngineMessageToProcess>,
    taskCommandsChannel: SendChannel<TaskEngineMessageToProcess>,
    workflowCommandsChannel: SendChannel<WorkflowEngineMessageToProcess>
) : ClientOutput {
    override val clientName = ClientName("client: inMemory")

    override val sendToTagEngineFn: SendToTagEngine = { msg: TagEngineMessage ->
        with(CoroutineScope(Dispatchers.IO)) {
            // As it's a back loop, we trigger it asynchronously to avoid deadlocks
            launch {
                tagCommandsChannel.send(InMemoryMessageToProcess(msg))
            }
        }
    }

    override val sendToWorkflowEngineFn: SendToWorkflowEngine = { msg: WorkflowEngineMessage, after: MillisDuration ->
        with(CoroutineScope(Dispatchers.IO)) {
            // As it's a back loop, we trigger it asynchronously to avoid deadlocks
            launch {
                delay(after.long)
                workflowCommandsChannel.send(InMemoryMessageToProcess(msg))
            }
        }
    }

    override val sendToTaskEngineFn: SendToTaskEngine = { msg: TaskEngineMessage, after: MillisDuration ->
        with(CoroutineScope(Dispatchers.IO)) {
            // As it's a back loop, we trigger it asynchronously to avoid deadlocks
            launch {
                delay(after.long)
                taskCommandsChannel.send(InMemoryMessageToProcess(msg))
            }
        }
    }
}
