package com.zenaton.engine.decisions

import com.zenaton.engine.common.attributes.*
import com.zenaton.engine.workflows.state.Action
import com.zenaton.engine.workflows.state.ActionId
import com.zenaton.engine.workflows.state.Branch

sealed class Message() {
    abstract val decisionId: DecisionId

    data class DecisionDispatched(
        override val decisionId: DecisionId,
        val workflowId: WorkflowId,
        val workflowName: String,
        val actions: Map<ActionId, Action> = mapOf(),
        val runningBranches: List<Branch> = listOf()
    ) : Message()

    data class DecisionCompleted(
        override val decisionId: DecisionId
    ) : Message()

    data class DecisionAttemptDispatched(
        override val decisionId: DecisionId,
        val attemptId: DecisionAttemptId
    ) : Message()

    data class DecisionAttemptStarted(
        override val decisionId: DecisionId,
        val attemptId: DecisionAttemptId
    ) : Message()

    data class DecisionAttemptCompleted(
        override val decisionId: DecisionId,
        val attemptId: DecisionAttemptId
    ) : Message()

    data class DecisionAttemptFailed(
        override val decisionId: DecisionId,
        val attemptId: DecisionAttemptId
    ) : Message()
}

