package io.infinitic.workflowManager.common.states

import io.infinitic.workflowManager.common.data.workflowTasks.WorkflowTaskId
import io.infinitic.workflowManager.common.data.workflows.WorkflowId
import io.infinitic.workflowManager.common.data.methods.Branch
import io.infinitic.workflowManager.common.data.properties.Properties
import io.infinitic.workflowManager.common.data.properties.PropertyStore
import io.infinitic.workflowManager.common.messages.ForWorkflowEngineMessage

sealed class State

data class WorkflowEngineState(
    val workflowId: WorkflowId,
    var parentWorkflowId: WorkflowId? = null,
    var ongoingWorkflowTaskId: WorkflowTaskId? = null,
    val bufferedMessages: MutableList<ForWorkflowEngineMessage> = mutableListOf(),
    val store: PropertyStore = PropertyStore(mutableMapOf()),
    val runningBranches: MutableList<Branch> = mutableListOf(),
    val currentProperties: Properties = Properties(mapOf())
) : State()
