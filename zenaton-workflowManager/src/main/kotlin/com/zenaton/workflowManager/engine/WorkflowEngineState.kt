package com.zenaton.workflowManager.engine

import com.zenaton.commons.data.interfaces.StateInterface
import com.zenaton.workflowManager.data.DecisionId
import com.zenaton.workflowManager.data.WorkflowId
import com.zenaton.workflowManager.data.state.Branch
import com.zenaton.workflowManager.data.state.Properties
import com.zenaton.workflowManager.data.state.Store
import com.zenaton.workflowManager.messages.envelopes.ForWorkflowEngineMessage

data class WorkflowEngineState(
    val workflowId: WorkflowId,
    var parentWorkflowId: WorkflowId? = null,
    var ongoingDecisionId: DecisionId? = null,
    val bufferedMessages: MutableList<ForWorkflowEngineMessage> = mutableListOf(),
    val store: Store = Store(),
    val runningBranches: MutableList<Branch> = mutableListOf(),
    val currentProperties: Properties = Properties()
) : StateInterface
