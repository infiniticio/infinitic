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

import io.infinitic.common.clients.messages.MethodCanceled
import io.infinitic.common.workflows.data.commands.CommandType
import io.infinitic.common.workflows.data.methodRuns.MethodRun
import io.infinitic.common.workflows.data.methodRuns.MethodRunId
import io.infinitic.common.workflows.data.workflows.WorkflowCancellationReason
import io.infinitic.common.workflows.data.workflows.WorkflowId
import io.infinitic.common.workflows.data.workflows.WorkflowName
import io.infinitic.common.workflows.engine.messages.CancelWorkflow
import io.infinitic.common.workflows.engine.messages.ChildMethodCanceled
import io.infinitic.common.workflows.engine.state.WorkflowState
import io.infinitic.exceptions.thisShouldNotHappen
import io.infinitic.workflows.engine.output.WorkflowEngineOutput
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

internal fun CoroutineScope.cancelWorkflow(
    output: WorkflowEngineOutput,
    state: WorkflowState,
    message: CancelWorkflow
) {
    when (message.methodRunId) {
        null -> {
            state.methodRuns.forEach {
                cancelMethodRun(output, state, it, message.reason)
            }

            // clean state
            state.removeAllMethodRuns()
        }
        else -> {
            state.getMethodRun(message.methodRunId!!)?. let { methodRun ->
                cancelMethodRun(output, state, methodRun, message.reason)

                // clean state
                state.removeMethodRun(methodRun)
            }
        }
    }
}

private fun CoroutineScope.cancelMethodRun(
    output: WorkflowEngineOutput,
    state: WorkflowState,
    methodRun: MethodRun,
    reason: WorkflowCancellationReason,
) {
    // inform waiting clients of cancellation
    methodRun.waitingClients.forEach {
        val workflowCanceled = MethodCanceled(
            recipientName = it,
            workflowId = state.workflowId,
            methodRunId = methodRun.methodRunId,
            emitterName = output.clientName
        )
        launch { output.sendEventsToClient(workflowCanceled) }
    }
    methodRun.waitingClients.clear()

    // inform parents of cancellation (if parent did not trigger the cancellation!)
    if (reason != WorkflowCancellationReason.CANCELED_BY_PARENT && methodRun.parentWorkflowId != null) {
        val childMethodCanceled = ChildMethodCanceled(
            workflowName = methodRun.parentWorkflowName ?: thisShouldNotHappen(),
            workflowId = methodRun.parentWorkflowId ?: thisShouldNotHappen(),
            methodRunId = methodRun.parentMethodRunId ?: thisShouldNotHappen(),
            childWorkflowName = state.workflowName,
            childWorkflowId = state.workflowId,
            childMethodRunId = methodRun.methodRunId,
            emitterName = output.clientName
        )
        launch { output.sendToWorkflowEngine(childMethodCanceled) }
    }

    // cancel children
    methodRun.pastCommands
        .filter { it.commandType == CommandType.DISPATCH_CHILD_WORKFLOW }
        .forEach {
            val cancelWorkflow = CancelWorkflow(
                workflowName = WorkflowName.from(it.commandName ?: thisShouldNotHappen()),
                workflowId = WorkflowId(it.commandId.toString()),
                methodRunId = MethodRunId.from(it.commandId),
                reason = WorkflowCancellationReason.CANCELED_BY_PARENT,
                emitterName = output.clientName
            )
            launch { output.sendToWorkflowEngine(cancelWorkflow) }
        }
}
