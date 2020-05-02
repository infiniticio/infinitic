package com.zenaton.engine.data.workflows

import com.zenaton.engine.data.StateInterface
import com.zenaton.engine.data.decisions.DecisionId
import com.zenaton.engine.data.workflows.states.Branch
import com.zenaton.engine.data.workflows.states.Properties
import com.zenaton.engine.data.workflows.states.Store
import com.zenaton.engine.topics.workflows.messages.WorkflowMessageInterface

data class WorkflowState(
    val workflowId: WorkflowId,
    var parentWorkflowId: WorkflowId? = null,
    var ongoingDecisionId: DecisionId? = null,
    val bufferedMessages: MutableList<WorkflowMessageInterface> = mutableListOf(),
    val store: Store = Store(),
    val runningBranches: MutableList<Branch> = mutableListOf(),
    val currentProperties: Properties = Properties()
) : StateInterface
