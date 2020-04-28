package com.zenaton.pulsar.topics.decisions.messages

import com.fasterxml.jackson.annotation.JsonInclude
import com.zenaton.engine.data.workflows.states.Branch
import com.zenaton.engine.data.workflows.states.Store

@JsonInclude(JsonInclude.Include.NON_NULL)
class PulsarDecisionMessage() {
    var type: String? = null
    /**
     * Decision
     */
    var decisionId: String? = null
    var decisionOutput: String? = null
    /**
     * Decision Attempts
     */
    var decisionAttemptId: String? = null
    var decisionAttemptOutput: String? = null
    var decisionAttemptError: String? = null
    /**
     * Workflows
     */
    var workflowId: String? = null
    var workflowName: String? = null
    /**
     * Dates
     */
    var dispatchedAt: String? = null
    var receivedAt: String? = null
    /**
     * Data
     */
    var branches: List<Branch> = listOf()
    var store: Store? = null
}
