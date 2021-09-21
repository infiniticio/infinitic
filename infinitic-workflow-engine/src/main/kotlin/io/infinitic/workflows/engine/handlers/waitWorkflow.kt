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

import io.infinitic.common.clients.messages.MethodAlreadyCompleted
import io.infinitic.common.workflows.engine.messages.WaitWorkflow
import io.infinitic.common.workflows.engine.state.WorkflowState
import io.infinitic.workflows.engine.output.WorkflowEngineOutput
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

internal fun CoroutineScope.waitWorkflow(
    output: WorkflowEngineOutput,
    state: WorkflowState,
    message: WaitWorkflow
) {
    when (val methodRun = state.getMethodRun(message.methodRunId)) {
        null -> {
            println(state.methodRuns)
            println(message)
            // this branch is already completed (can not be canceled, the state would be deleted)
            val workflowAlreadyCompleted = MethodAlreadyCompleted(
                message.clientName,
                message.workflowId,
                message.methodRunId
            )
            launch { output.sendEventsToClient(workflowAlreadyCompleted) }
        }
        else -> methodRun.waitingClients.add(message.clientName)
    }
}
