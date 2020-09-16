package io.infinitic.workflowManager.common.states

import io.infinitic.workflowManager.common.data.workflowTasks.WorkflowTaskId
import io.infinitic.workflowManager.common.data.workflows.WorkflowId
import io.infinitic.workflowManager.common.data.methodRuns.MethodRun
import io.infinitic.workflowManager.common.data.properties.Properties
import io.infinitic.workflowManager.common.data.properties.PropertyStore
import io.infinitic.workflowManager.common.data.workflowTasks.WorkflowEventIndex
import io.infinitic.workflowManager.common.messages.ForWorkflowEngineMessage

sealed class State

data class WorkflowState(
    val workflowId: WorkflowId,
    var currentWorkflowTaskId: WorkflowTaskId? = null,
    var currentEventIndex: WorkflowEventIndex = WorkflowEventIndex(-1),
    val currentMethodRuns: MutableList<MethodRun>,
    val currentProperties: Properties = Properties(mutableMapOf()),
    val propertyStore: PropertyStore = PropertyStore(mutableMapOf()),
    val bufferedMessages: MutableList<ForWorkflowEngineMessage> = mutableListOf()
) : State()
