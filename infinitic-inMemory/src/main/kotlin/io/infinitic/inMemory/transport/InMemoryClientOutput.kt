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

import io.infinitic.client.output.ClientOutput
import io.infinitic.common.clients.data.ClientName
import io.infinitic.common.tags.messages.TagEngineMessage
import io.infinitic.common.tasks.engine.messages.TaskEngineMessage
import io.infinitic.common.workflows.engine.messages.WorkflowEngineMessage
import io.infinitic.tags.engine.input.TagEngineMessageToProcess
import io.infinitic.tasks.engine.input.TaskEngineMessageToProcess
import io.infinitic.workflows.engine.input.WorkflowEngineMessageToProcess
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.launch

class InMemoryClientOutput(
    private val tagCommandsChannel: SendChannel<TagEngineMessageToProcess>,
    private val taskCommandsChannel: SendChannel<TaskEngineMessageToProcess>,
    private val workflowCommandsChannel: SendChannel<WorkflowEngineMessageToProcess>
) : ClientOutput {
    override val clientName = ClientName("client: inMemory")

    override suspend fun sendToWorkflowEngine(workflowEngineMessage: WorkflowEngineMessage) {
        with(CoroutineScope(Dispatchers.IO)) {
            // As it's a back loop, we trigger it asynchronously to avoid deadlocks
            launch {
                workflowCommandsChannel.send(InMemoryMessageToProcess(workflowEngineMessage))
            }
        }
    }

    override suspend fun sendToTaskEngine(taskEngineMessage: TaskEngineMessage) {
        with(CoroutineScope(Dispatchers.IO)) {
            // As it's a back loop, we trigger it asynchronously to avoid deadlocks
            launch {
                taskCommandsChannel.send(InMemoryMessageToProcess(taskEngineMessage))
            }
        }
    }

    override suspend fun sendToTagEngine(tagEngineMessage: TagEngineMessage) {
        with(CoroutineScope(Dispatchers.IO)) {
            // As it's a back loop, we trigger it asynchronously to avoid deadlocks
            launch {
                tagCommandsChannel.send(InMemoryMessageToProcess(tagEngineMessage))
            }
        }
    }
}
