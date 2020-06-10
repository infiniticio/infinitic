package com.zenaton.workflowManager.topics.workflows.state

import com.zenaton.commons.data.interfaces.StateInterface
import com.zenaton.decisionmanager.data.DecisionId
import com.zenaton.workflowManager.data.WorkflowId
import com.zenaton.workflowManager.messages.envelopes.ForWorkflowEngineMessage

data class WorkflowState(
    val workflowId: WorkflowId,
    var parentWorkflowId: WorkflowId? = null,
    var ongoingDecisionId: DecisionId? = null,
    val bufferedMessages: MutableList<ForWorkflowEngineMessage> = mutableListOf(),
    val store: Store = Store(),
    val runningBranches: MutableList<Branch> = mutableListOf(),
    val currentProperties: Properties = Properties()
) : StateInterface
