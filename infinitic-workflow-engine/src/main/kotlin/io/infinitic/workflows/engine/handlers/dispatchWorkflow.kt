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

import io.infinitic.common.workflows.data.methodRuns.MethodRun
import io.infinitic.common.workflows.data.methodRuns.MethodRunId
import io.infinitic.common.workflows.data.methodRuns.MethodRunPosition
import io.infinitic.common.workflows.data.workflowTasks.WorkflowTaskIndex
import io.infinitic.common.workflows.engine.messages.DispatchWorkflow
import io.infinitic.common.workflows.engine.state.WorkflowState
import io.infinitic.workflows.engine.helpers.dispatchWorkflowTask
import io.infinitic.workflows.engine.output.WorkflowEngineOutput
import kotlinx.coroutines.CoroutineScope

internal fun CoroutineScope.dispatchWorkflow(
    workflowEngineOutput: WorkflowEngineOutput,
    message: DispatchWorkflow
): WorkflowState {
    val methodRun = MethodRun(
        methodRunId = MethodRunId.from(message.workflowId),
        waitingClients = when (message.clientWaiting) {
            true -> mutableSetOf(message.emitterName)
            false -> mutableSetOf()
        },
        parentWorkflowId = message.parentWorkflowId,
        parentWorkflowName = message.parentWorkflowName,
        parentMethodRunId = message.parentMethodRunId,
        methodName = message.methodName,
        methodParameterTypes = message.methodParameterTypes,
        methodParameters = message.methodParameters,
        workflowTaskIndexAtStart = WorkflowTaskIndex(0),
        propertiesNameHashAtStart = mapOf()
    )

    val state = WorkflowState(
        lastMessageId = message.messageId,
        workflowId = message.workflowId,
        workflowName = message.workflowName,
        workflowTags = message.workflowTags,
        workflowOptions = message.workflowOptions,
        workflowMeta = message.workflowMeta,
        methodRuns = mutableListOf(methodRun)
    )

    dispatchWorkflowTask(workflowEngineOutput, state, methodRun, MethodRunPosition.new())

    return state
}
