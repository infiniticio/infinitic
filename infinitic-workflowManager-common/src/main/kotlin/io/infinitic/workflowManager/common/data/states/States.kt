package io.infinitic.workflowManager.common.data.states

import io.infinitic.workflowManager.common.data.workflowTasks.WorkflowTaskId
import io.infinitic.workflowManager.common.data.workflows.WorkflowId
import io.infinitic.workflowManager.common.data.methodRuns.MethodRun
import io.infinitic.workflowManager.common.data.properties.Properties
import io.infinitic.workflowManager.common.data.properties.PropertyStore
import io.infinitic.workflowManager.common.data.workflows.WorkflowMessageIndex
import io.infinitic.workflowManager.common.data.workflows.WorkflowName
import io.infinitic.workflowManager.common.data.workflows.WorkflowOptions
import io.infinitic.workflowManager.common.messages.ForWorkflowEngineMessage

sealed class State

data class WorkflowState(
    val workflowId: WorkflowId,
    val parentWorkflowId: WorkflowId? = null,
    val workflowName: WorkflowName,
    val workflowOptions: WorkflowOptions,
    var currentWorkflowTaskId: WorkflowTaskId? = null,
    var currentMessageIndex: WorkflowMessageIndex = WorkflowMessageIndex(0),
    val currentMethodRuns: MutableList<MethodRun>,
    val currentProperties: Properties = Properties(mutableMapOf()),
    val propertyStore: PropertyStore = PropertyStore(mutableMapOf()),
    val bufferedMessages: MutableList<ForWorkflowEngineMessage> = mutableListOf()
) : State()
