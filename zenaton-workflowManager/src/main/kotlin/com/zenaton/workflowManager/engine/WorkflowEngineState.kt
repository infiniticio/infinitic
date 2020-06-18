package com.zenaton.workflowManager.engine

import com.zenaton.common.data.interfaces.StateInterface
import com.zenaton.workflowManager.data.DecisionId
import com.zenaton.workflowManager.data.WorkflowId
import com.zenaton.workflowManager.data.branches.Branch
import com.zenaton.workflowManager.data.properties.Properties
import com.zenaton.workflowManager.data.properties.PropertyStore
import com.zenaton.workflowManager.messages.envelopes.ForWorkflowEngineMessage

data class WorkflowEngineState(
    val workflowId: WorkflowId,
    var parentWorkflowId: WorkflowId? = null,
    var ongoingDecisionId: DecisionId? = null,
    val bufferedMessages: MutableList<ForWorkflowEngineMessage> = mutableListOf(),
    val store: PropertyStore = PropertyStore(),
    val runningBranches: MutableList<Branch> = mutableListOf(),
    val currentProperties: Properties = Properties()
) : StateInterface
