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

import io.infinitic.common.clients.data.ClientName
import io.infinitic.common.data.MillisDuration
import io.infinitic.common.data.MillisInstant
import io.infinitic.common.data.methods.MethodName
import io.infinitic.common.data.methods.MethodParameterTypes
import io.infinitic.common.data.methods.MethodParameters
import io.infinitic.common.tasks.data.TaskId
import io.infinitic.common.tasks.data.TaskMeta
import io.infinitic.common.tasks.data.TaskName
import io.infinitic.common.tasks.data.TaskOptions
import io.infinitic.common.tasks.engine.messages.DispatchTask
import io.infinitic.common.workflows.data.methodRuns.MethodRun
import io.infinitic.common.workflows.data.methodRuns.MethodRunPosition
import io.infinitic.common.workflows.data.workflowTasks.WorkflowTask
import io.infinitic.common.workflows.data.workflowTasks.WorkflowTaskId
import io.infinitic.common.workflows.data.workflowTasks.WorkflowTaskParameters
import io.infinitic.common.workflows.data.workflowTasks.plus
import io.infinitic.common.workflows.engine.messages.WorkflowTaskDispatched
import io.infinitic.common.workflows.engine.state.WorkflowState
import io.infinitic.workflows.engine.transport.WorkflowEngineOutput

suspend fun dispatchWorkflowTask(
    workflowEngineOutput: WorkflowEngineOutput,
    state: WorkflowState,
    methodRun: MethodRun,
    branchPosition: MethodRunPosition = MethodRunPosition("")
) {
    state.workflowTaskIndex = state.workflowTaskIndex + 1

    // defines workflow task input
    val workflowTaskInput = WorkflowTaskParameters(
        workflowId = state.workflowId,
        workflowName = state.workflowName,
        workflowOptions = state.workflowOptions,
        workflowPropertiesHashValue = state.propertiesHashValue, // TODO filterStore(state.propertyStore, listOf(methodRun))
        workflowTaskIndex = state.workflowTaskIndex,
        methodRun = methodRun,
        targetPosition = branchPosition
    )

    // defines workflow task
    val workflowTaskId = WorkflowTaskId()

    val workflowTask = DispatchTask(
        clientName = ClientName("workflow engine"),
        clientWaiting = false,
        taskId = TaskId("$workflowTaskId"),
        taskName = TaskName(WorkflowTask::class.java.name),
        methodName = MethodName(WorkflowTask.DEFAULT_METHOD),
        methodParameterTypes = MethodParameterTypes(listOf(WorkflowTaskParameters::class.java.name)),
        methodParameters = MethodParameters.from(workflowTaskInput),
        workflowId = state.workflowId,
        methodRunId = methodRun.methodRunId,
        taskOptions = TaskOptions(),
        taskMeta = TaskMeta.from(mapOf(WorkflowTask.META_WORKFLOW_NAME to "${state.workflowName}"))
    )

    // dispatch workflow task
    workflowEngineOutput.sendToTaskEngine(
        state,
        workflowTask,
        MillisDuration(0)
    )

    // log event
    workflowEngineOutput.sendToWorkflowEngine(
        state,
        WorkflowTaskDispatched(
            workflowTaskId = workflowTaskId,
            workflowId = state.workflowId,
            workflowName = state.workflowName,
            workflowTaskParameters = workflowTaskInput
        ),
        MillisDuration(0)
    )

    state.runningWorkflowTaskId = workflowTaskId
    state.runningWorkflowTaskInstant = MillisInstant.now()
}
