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

import io.infinitic.common.workflows.data.commands.CommandType
import io.infinitic.common.workflows.data.workflows.WorkflowId
import io.infinitic.common.workflows.data.workflows.WorkflowName
import io.infinitic.common.workflows.data.workflows.WorkflowStatus
import io.infinitic.common.workflows.engine.messages.CancelWorkflow
import io.infinitic.common.workflows.engine.messages.ChildWorkflowCanceled
import io.infinitic.common.workflows.engine.state.WorkflowState
import io.infinitic.workflows.engine.output.WorkflowEngineOutput
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

internal fun CoroutineScope.cancelWorkflow(
    output: WorkflowEngineOutput,
    state: WorkflowState
) {
    state.workflowStatus = WorkflowStatus.CANCELED

    state.methodRuns.forEach { methodRun ->
        // tell parents
        if (methodRun.parentWorkflowId != null) {
            val childWorkflowCanceled = ChildWorkflowCanceled(
                workflowId = methodRun.parentWorkflowId!!,
                workflowName = methodRun.parentWorkflowName!!,
                methodRunId = methodRun.parentMethodRunId!!,
                childWorkflowId = state.workflowId,
                childWorkflowName = state.workflowName
            )
            launch { output.sendToWorkflowEngine(childWorkflowCanceled) }
        }

        // cancel children
        methodRun.pastCommands
            .filter { it.commandType == CommandType.DISPATCH_CHILD_WORKFLOW }
            .forEach {
                val cancelWorkflow = CancelWorkflow(
                    workflowId = WorkflowId(it.commandId.id),
                    workflowName = WorkflowName("${it.commandName}")
                )
                launch { output.sendToWorkflowEngine(cancelWorkflow) }
            }
    }
}
