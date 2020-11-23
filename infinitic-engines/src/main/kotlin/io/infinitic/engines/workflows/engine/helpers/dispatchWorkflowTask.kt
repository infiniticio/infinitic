// "Commons Clause" License Condition v1.0
//
// The Software is provided to you by the Licensor under the License, as defined
// below, subject to the following condition.
//
// Without limiting other conditions in the License, the grant of rights under the
// License will not include, and the License does not grant to you, the right to
// Sell the Software.
//
// For purposes of the foregoing, “Sell” means practicing any or all of the rights
// granted to you under the License to provide to third parties, for a fee or
// other consideration (including without limitation fees for hosting or
// consulting/ support services related to the Software), a product or service
// whose value derives, entirely or substantially, from the functionality of the
// Software. Any license notice or attribution required by the License must also
// include this Commons Clause License Condition notice.
//
// Software: Infinitic
//
// License: MIT License (https://opensource.org/licenses/MIT)
//
// Licensor: infinitic.io

package io.infinitic.engines.workflows.engine.helpers

import io.infinitic.common.data.interfaces.inc
import io.infinitic.common.data.methods.MethodInput
import io.infinitic.common.data.methods.MethodName
import io.infinitic.common.data.methods.MethodParameterTypes
import io.infinitic.common.tasks.data.TaskId
import io.infinitic.common.tasks.data.TaskMeta
import io.infinitic.common.tasks.data.TaskName
import io.infinitic.common.tasks.data.TaskOptions
import io.infinitic.common.tasks.messages.DispatchTask
import io.infinitic.common.workflows.data.methodRuns.MethodRun
import io.infinitic.common.workflows.data.methodRuns.MethodRunPosition
import io.infinitic.common.workflows.data.workflowTasks.WorkflowTask
import io.infinitic.common.workflows.data.workflowTasks.WorkflowTaskId
import io.infinitic.common.workflows.data.workflowTasks.WorkflowTaskInput
import io.infinitic.common.workflows.messages.WorkflowTaskDispatched
import io.infinitic.common.workflows.state.WorkflowState
import io.infinitic.common.SendToTaskEngine
import io.infinitic.common.SendToWorkflowEngine
import io.infinitic.engines.workflows.engine.WorkflowEngine

suspend fun dispatchWorkflowTask(
    sendToWorkflowEngine: SendToWorkflowEngine,
    sendToTaskEngine: SendToTaskEngine,
    state: WorkflowState,
    methodRun: MethodRun,
    branchPosition: MethodRunPosition = MethodRunPosition("")
) {
    state.workflowTaskIndex++

    // defines workflow task input
    val workflowTaskInput = WorkflowTaskInput(
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
        taskId = TaskId("$workflowTaskId"),
        taskName = TaskName(WorkflowTask::class.java.name),
        methodName = MethodName(WorkflowTask.DEFAULT_METHOD),
        methodParameterTypes = MethodParameterTypes(listOf(WorkflowTaskInput::class.java.name)),
        methodInput = MethodInput.from(workflowTaskInput),
        taskOptions = TaskOptions(),
        taskMeta = TaskMeta.from(
            mapOf(
                WorkflowEngine.META_WORKFLOW_ID to "${state.workflowId}",
                WorkflowEngine.META_METHOD_RUN_ID to "${methodRun.methodRunId}"
            )
        )
    )

    // dispatch workflow task
    sendToTaskEngine(workflowTask, 0F)

    // log event
    sendToWorkflowEngine(
        WorkflowTaskDispatched(
            workflowTaskId = workflowTaskId,
            workflowId = state.workflowId,
            workflowName = state.workflowName,
            workflowTaskInput = workflowTaskInput
        ),
        0F
    )

    state.runningWorkflowTaskId = workflowTaskId
}