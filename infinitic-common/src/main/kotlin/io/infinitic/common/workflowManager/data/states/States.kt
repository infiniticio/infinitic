package io.infinitic.common.workflowManager.data.states

import io.infinitic.common.workflowManager.data.workflowTasks.WorkflowTaskId
import io.infinitic.common.workflowManager.data.workflows.WorkflowId
import io.infinitic.common.workflowManager.data.methodRuns.MethodRun
import io.infinitic.common.workflowManager.data.properties.Properties
import io.infinitic.common.workflowManager.data.properties.PropertyStore
import io.infinitic.common.workflowManager.data.workflows.WorkflowMessageIndex
import io.infinitic.common.workflowManager.data.workflows.WorkflowMeta
import io.infinitic.common.workflowManager.data.workflows.WorkflowName
import io.infinitic.common.workflowManager.data.workflows.WorkflowOptions
import io.infinitic.common.workflowManager.messages.ForWorkflowEngineMessage

sealed class State

data class WorkflowState(
    val workflowId: WorkflowId,
    val parentWorkflowId: WorkflowId? = null,
    val workflowName: WorkflowName,
    val workflowOptions: WorkflowOptions,
    val workflowMeta: WorkflowMeta,
    var currentWorkflowTaskId: WorkflowTaskId? = null,
    var currentMessageIndex: WorkflowMessageIndex = WorkflowMessageIndex(0),
    val currentMethodRuns: MutableList<MethodRun>,
    val currentProperties: Properties = Properties(mutableMapOf()),
    val propertyStore: PropertyStore = PropertyStore(mutableMapOf()),
    val bufferedMessages: MutableList<ForWorkflowEngineMessage> = mutableListOf()
) : State()
