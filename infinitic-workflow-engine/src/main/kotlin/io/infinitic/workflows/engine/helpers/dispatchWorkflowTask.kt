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

package io.infinitic.workflows.engine.helpers

import io.infinitic.common.data.MillisInstant
import io.infinitic.common.data.methods.MethodName
import io.infinitic.common.data.methods.MethodParameterTypes
import io.infinitic.common.data.methods.MethodParameters
import io.infinitic.common.tasks.data.TaskId
import io.infinitic.common.tasks.data.TaskMeta
import io.infinitic.common.tasks.data.TaskName
import io.infinitic.common.tasks.engine.messages.DispatchTask
import io.infinitic.common.workflows.data.methodRuns.MethodRun
import io.infinitic.common.workflows.data.methodRuns.MethodRunPosition
import io.infinitic.common.workflows.data.workflowTasks.WorkflowTask
import io.infinitic.common.workflows.data.workflowTasks.WorkflowTaskParameters
import io.infinitic.common.workflows.data.workflowTasks.plus
import io.infinitic.common.workflows.engine.state.WorkflowState
import io.infinitic.tasks.TaskOptions
import io.infinitic.workflows.engine.output.WorkflowEngineOutput
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

internal fun CoroutineScope.dispatchWorkflowTask(
    output: WorkflowEngineOutput,
    state: WorkflowState,
    methodRun: MethodRun,
    methodRunPosition: MethodRunPosition
) {
    state.workflowTaskIndex = state.workflowTaskIndex + 1

    // defines workflow task input
    val workflowTaskParameters = WorkflowTaskParameters(
        workflowId = state.workflowId,
        workflowName = state.workflowName,
        workflowOptions = state.workflowOptions,
        workflowTags = state.workflowTags,
        workflowMeta = state.workflowMeta,
        workflowPropertiesHashValue = state.propertiesHashValue, // TODO filterStore(state.propertyStore, listOf(methodRun))
        workflowTaskIndex = state.workflowTaskIndex,
        methodRun = methodRun
    )

    // defines workflow task
    val workflowTask = DispatchTask(
        taskName = TaskName(WorkflowTask::class.java.name),
        taskId = TaskId(),
        taskOptions = TaskOptions(),
        clientWaiting = false,
        methodName = MethodName(WorkflowTask::handle.name),
        methodParameterTypes = MethodParameterTypes(listOf(WorkflowTaskParameters::class.java.name)),
        methodParameters = MethodParameters.from(workflowTaskParameters),
        workflowId = state.workflowId,
        workflowName = state.workflowName,
        methodRunId = methodRun.methodRunId,
        taskTags = setOf(),
        taskMeta = TaskMeta(),
        emitterName = output.clientName
    )

    // dispatch workflow task
    launch { output.sendToTaskEngine(workflowTask) }

    with(state) {
        runningWorkflowTaskId = workflowTask.taskId
        // do not update runningWorkflowTaskInstant if it is a retry
        if (runningMethodRunId != methodRun.methodRunId || runningMethodRunPosition != methodRunPosition) {
            runningWorkflowTaskInstant = MillisInstant.now()
            runningMethodRunId = methodRun.methodRunId
            runningMethodRunPosition = methodRunPosition
        }
    }
}
