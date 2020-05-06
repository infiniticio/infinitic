package com.zenaton.engine.workflows.data

import com.zenaton.engine.decisions.data.DecisionId
import com.zenaton.engine.interfaces.data.StateInterface
import com.zenaton.engine.workflows.data.states.Branch
import com.zenaton.engine.workflows.data.states.Properties
import com.zenaton.engine.workflows.data.states.Store
import com.zenaton.engine.workflows.interfaces.WorkflowMessageInterface

data class WorkflowState(
    val workflowId: WorkflowId,
    var parentWorkflowId: WorkflowId? = null,
    var ongoingDecisionId: DecisionId? = null,
    val bufferedMessages: MutableList<WorkflowMessageInterface> = mutableListOf(),
    val store: Store = Store(),
    val runningBranches: MutableList<Branch> = mutableListOf(),
    val currentProperties: Properties = Properties()
) : StateInterface
