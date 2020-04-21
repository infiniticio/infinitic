package com.zenaton.engine.decisions

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.zenaton.engine.attributes.decisions.DecisionAttemptError
import com.zenaton.engine.attributes.decisions.DecisionAttemptId
import com.zenaton.engine.attributes.decisions.DecisionId
import com.zenaton.engine.attributes.decisions.DecisionOutput
import com.zenaton.engine.attributes.workflows.WorkflowId
import com.zenaton.engine.attributes.workflows.WorkflowName
import com.zenaton.engine.attributes.workflows.states.Action
import com.zenaton.engine.attributes.workflows.states.ActionId
import com.zenaton.engine.attributes.workflows.states.Branch

sealed class DecisionMessage(val type: String, open var decisionId: DecisionId) {
    fun getStateKey() = decisionId.id
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class DecisionDispatched(
    override var decisionId: DecisionId,
    val workflowId: WorkflowId,
    val workflowName: WorkflowName,
    val actions: Map<ActionId, Action> = mapOf(),
    val runningBranches: List<Branch> = listOf()
) : DecisionMessage("DecisionDispatched", decisionId)

@JsonIgnoreProperties(ignoreUnknown = true)
data class DecisionCompleted(
    override var decisionId: DecisionId,
    val decisionOutput: DecisionOutput
) : DecisionMessage("DecisionCompleted", decisionId)

@JsonIgnoreProperties(ignoreUnknown = true)
data class DecisionAttemptDispatched(
    override var decisionId: DecisionId,
    val decisionAttemptId: DecisionAttemptId
) : DecisionMessage("DecisionAttemptDispatched", decisionId)

@JsonIgnoreProperties(ignoreUnknown = true)
data class DecisionAttemptStarted(
    override var decisionId: DecisionId,
    val decisionAttemptId: DecisionAttemptId,
    val workflowId: WorkflowId,
    val workflowName: WorkflowName,
    val actions: Map<ActionId, Action> = mapOf(),
    val runningBranches: List<Branch> = listOf()
) : DecisionMessage("DecisionAttemptStarted", decisionId)

@JsonIgnoreProperties(ignoreUnknown = true)
data class DecisionAttemptCompleted(
    override var decisionId: DecisionId,
    val decisionAttemptId: DecisionAttemptId,
    val decisionOutput: DecisionOutput
) : DecisionMessage("DecisionAttemptCompleted", decisionId)

@JsonIgnoreProperties(ignoreUnknown = true)
data class DecisionAttemptFailed(
    override var decisionId: DecisionId,
    val decisionAttemptId: DecisionAttemptId,
    val decisionAttemptError: DecisionAttemptError

) : DecisionMessage("DecisionAttemptFailed", decisionId)
