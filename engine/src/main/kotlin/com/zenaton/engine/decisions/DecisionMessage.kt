package com.zenaton.engine.decisions

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.zenaton.engine.attributes.decisions.DecisionAttemptError
import com.zenaton.engine.attributes.decisions.DecisionAttemptId
import com.zenaton.engine.attributes.decisions.DecisionId
import com.zenaton.engine.attributes.decisions.DecisionOutput
import com.zenaton.engine.attributes.workflows.WorkflowId
import com.zenaton.engine.attributes.workflows.WorkflowName
import com.zenaton.engine.attributes.workflows.states.Action
import com.zenaton.engine.attributes.workflows.states.ActionId
import com.zenaton.engine.attributes.workflows.states.Branch
import com.zenaton.engine.workflows.DecisionCompleted

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes(
    JsonSubTypes.Type(value = DecisionDispatched::class, name = "DecisionDispatched"),
    JsonSubTypes.Type(value = DecisionCompleted::class, name = "DecisionCompleted"),
    JsonSubTypes.Type(value = DecisionAttemptDispatched::class, name = "DecisionAttemptDispatched"),
    JsonSubTypes.Type(value = DecisionAttemptStarted::class, name = "DecisionAttemptStarted"),
    JsonSubTypes.Type(value = DecisionAttemptCompleted::class, name = "DecisionAttemptCompleted")
)
sealed class DecisionMessage(open var decisionId: DecisionId) {
    @JsonIgnore
    fun getStateKey() = decisionId.id
}

data class DecisionDispatched(
    override var decisionId: DecisionId,
    val workflowId: WorkflowId,
    val workflowName: WorkflowName,
    val actions: Map<ActionId, Action> = mapOf(),
    val runningBranches: List<Branch> = listOf()
) : DecisionMessage(decisionId)

data class DecisionCompleted(
    override var decisionId: DecisionId,
    val decisionOutput: DecisionOutput
) : DecisionMessage(decisionId)

data class DecisionAttemptDispatched(
    override var decisionId: DecisionId,
    val decisionAttemptId: DecisionAttemptId
) : DecisionMessage(decisionId)

data class DecisionAttemptStarted(
    override var decisionId: DecisionId,
    val decisionAttemptId: DecisionAttemptId,
    val workflowId: WorkflowId,
    val workflowName: WorkflowName,
    val actions: Map<ActionId, Action> = mapOf(),
    val runningBranches: List<Branch> = listOf()
) : DecisionMessage(decisionId)

data class DecisionAttemptCompleted(
    override var decisionId: DecisionId,
    val decisionAttemptId: DecisionAttemptId,
    val decisionOutput: DecisionOutput
) : DecisionMessage(decisionId)

data class DecisionAttemptFailed(
    override var decisionId: DecisionId,
    val decisionAttemptId: DecisionAttemptId,
    val decisionAttemptError: DecisionAttemptError

) : DecisionMessage(decisionId)
