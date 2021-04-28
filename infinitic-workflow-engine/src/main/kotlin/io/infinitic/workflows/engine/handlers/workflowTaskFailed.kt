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

import io.infinitic.common.clients.messages.WorkflowFailed
import io.infinitic.common.workflows.data.commands.CommandCanceled
import io.infinitic.common.workflows.data.commands.CommandCompleted
import io.infinitic.common.workflows.data.commands.CommandId
import io.infinitic.common.workflows.data.commands.CommandOngoing
import io.infinitic.common.workflows.data.commands.CommandOngoingFailure
import io.infinitic.common.workflows.engine.messages.ChildWorkflowFailed
import io.infinitic.common.workflows.engine.messages.TaskFailed
import io.infinitic.common.workflows.engine.state.WorkflowState
import io.infinitic.exceptions.thisShouldNotHappen
import io.infinitic.workflows.engine.output.WorkflowEngineOutput

internal suspend fun workflowTaskFailed(
    output: WorkflowEngineOutput,
    state: WorkflowState,
    msg: TaskFailed
) {
    // if on main path, forward the error
    if (state.isRunningWorkflowTaskOnMainPath()) {
        val methodRun = state.getRunningMethodRun()

        // if the error is due to a a command failure, we enrich the error
        val error = when (msg.error.errorCause == null && msg.error.id != null) {
            true -> msg.error.copy(
                errorCause = when (val commandStatus = methodRun.getPastCommand(CommandId(msg.error.id!!)).commandStatus) {
                    is CommandCompleted -> thisShouldNotHappen()
                    CommandOngoing -> thisShouldNotHappen()
                    is CommandOngoingFailure -> commandStatus.error
                    is CommandCanceled -> null
                }
            )
            false -> msg.error
        }

        // send to waiting clients
        methodRun.waitingClients.forEach {
            val workflowFailed = WorkflowFailed(it, state.workflowId, error)
            output.sendEventsToClient(workflowFailed)
        }

        // send to parent workflow
        methodRun.parentWorkflowId?. run {
            val childWorkflowFailed = ChildWorkflowFailed(
                workflowId = methodRun.parentWorkflowId!!,
                workflowName = methodRun.parentWorkflowName!!,
                methodRunId = methodRun.parentMethodRunId!!,
                childWorkflowId = state.workflowId,
                childWorkflowError = error
            )
            output.sendToWorkflowEngine(childWorkflowFailed)
        }
    }
}
