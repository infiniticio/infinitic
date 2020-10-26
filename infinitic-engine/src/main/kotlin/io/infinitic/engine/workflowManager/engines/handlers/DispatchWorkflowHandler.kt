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

package io.infinitic.engine.workflowManager.engines.handlers

import io.infinitic.common.tasks.Constants
import io.infinitic.messaging.api.dispatcher.Dispatcher
import io.infinitic.common.tasks.data.TaskId
import io.infinitic.common.tasks.data.MethodInput
import io.infinitic.common.tasks.data.MethodName
import io.infinitic.common.tasks.data.MethodParameterTypes
import io.infinitic.common.tasks.data.TaskMeta
import io.infinitic.common.tasks.data.TaskName
import io.infinitic.common.tasks.data.TaskOptions
import io.infinitic.common.tasks.messages.DispatchTask
import io.infinitic.common.workflows.data.methodRuns.MethodRun
import io.infinitic.common.workflows.data.properties.PropertiesHashValue
import io.infinitic.common.workflows.data.workflowTasks.WorkflowTask
import io.infinitic.common.workflows.data.workflowTasks.WorkflowTaskId
import io.infinitic.common.workflows.data.workflowTasks.WorkflowTaskIndex
import io.infinitic.common.workflows.data.workflowTasks.WorkflowTaskInput
import io.infinitic.common.workflows.messages.WorkflowTaskDispatched
import io.infinitic.common.workflows.messages.DispatchWorkflow
import io.infinitic.common.workflows.data.states.WorkflowState
import io.infinitic.engine.workflowManager.engines.WorkflowEngine

class DispatchWorkflowHandler(
    override val dispatcher: Dispatcher
) : MsgHandler(dispatcher) {
    suspend fun handle(msg: DispatchWorkflow): WorkflowState {
        // defines method to run
        val methodRun = MethodRun(
            isMain = true,
            parentWorkflowId = msg.parentWorkflowId,
            parentMethodRunId = msg.parentMethodRunId,
            methodName = msg.methodName,
            methodParameterTypes = msg.methodParameterTypes,
            methodInput = msg.methodInput,
            workflowTaskIndexAtStart = WorkflowTaskIndex(0)
        )

        // defines workflow task input
        val workflowTaskInput = WorkflowTaskInput(
            workflowId = msg.workflowId,
            workflowName = msg.workflowName,
            workflowOptions = msg.workflowOptions,
            workflowPropertiesHashValue = PropertiesHashValue(),
            workflowTaskIndex = WorkflowTaskIndex(0),
            methodRun = methodRun
        )

        // defines workflow task
        val workflowTaskId = WorkflowTaskId()

        val workflowTask = DispatchTask(
            taskId = TaskId("$workflowTaskId"),
            taskName = TaskName(WorkflowTask::class.java.name),
            methodName = MethodName(Constants.WORKFLOW_TASK_METHOD),
            methodParameterTypes = MethodParameterTypes(listOf(WorkflowTaskInput::class.java.name)),
            methodInput = MethodInput(workflowTaskInput),
            taskOptions = TaskOptions(),
            taskMeta = TaskMeta(
                mapOf(
                    WorkflowEngine.META_WORKFLOW_ID to "${msg.workflowId}",
                    WorkflowEngine.META_METHOD_RUN_ID to "${methodRun.methodRunId}"
                )
            )
        )

        // dispatch workflow task
        dispatcher.toTaskEngine(workflowTask)

        // log event
        dispatcher.toWorkflowEngine(
            WorkflowTaskDispatched(
                workflowId = msg.workflowId,
                workflowTaskId = workflowTaskId,
                workflowName = msg.workflowName,
                workflowTaskInput = workflowTaskInput
            )
        )

        // initialize state
        return WorkflowState(
            workflowId = msg.workflowId,
            workflowName = msg.workflowName,
            workflowOptions = msg.workflowOptions,
            workflowMeta = msg.workflowMeta,
            currentWorkflowTaskId = workflowTaskId,
            currentMethodRuns = mutableListOf(methodRun)
        )
    }
}
