package com.zenaton.workflowManager.states

import com.zenaton.workflowManager.avroConverter.AvroConverter
import com.zenaton.workflowManager.data.decisions.DecisionId
import com.zenaton.workflowManager.data.WorkflowId
import com.zenaton.workflowManager.data.branches.Branch
import com.zenaton.workflowManager.data.properties.Properties
import com.zenaton.workflowManager.data.properties.PropertyStore
import com.zenaton.workflowManager.messages.ForWorkflowEngineMessage

sealed class State

data class WorkflowEngineState(
    val workflowId: WorkflowId,
    var parentWorkflowId: WorkflowId? = null,
    var ongoingDecisionId: DecisionId? = null,
    val bufferedMessages: MutableList<ForWorkflowEngineMessage> = mutableListOf(),
    val store: PropertyStore = PropertyStore(mutableMapOf()),
    val runningBranches: MutableList<Branch> = mutableListOf(),
    val currentProperties: Properties = Properties(mapOf())
) : State() {
    fun deepCopy() = AvroConverter.fromStorage(AvroConverter.toStorage(this))
}
