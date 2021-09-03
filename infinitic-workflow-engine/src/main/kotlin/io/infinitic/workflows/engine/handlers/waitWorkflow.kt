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

package io.infinitic.workflows.engine.handlers

import io.infinitic.common.clients.messages.WorkflowAlreadyCompleted
import io.infinitic.common.workflows.engine.messages.WaitWorkflow
import io.infinitic.common.workflows.engine.state.WorkflowState
import io.infinitic.workflows.engine.output.WorkflowEngineOutput
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

internal fun CoroutineScope.waitWorkflow(output: WorkflowEngineOutput, state: WorkflowState, msg: WaitWorkflow) {
    when (val main = state.getMainMethodRun()) {
        null -> {
            // main branch is already completed (can not be canceled, the state would be deleted)
            val workflowAlreadyCompleted = WorkflowAlreadyCompleted(msg.clientName, msg.workflowId)
            launch { output.sendEventsToClient(workflowAlreadyCompleted) }
        }
        else -> main.waitingClients.add(msg.clientName)
    }
}
