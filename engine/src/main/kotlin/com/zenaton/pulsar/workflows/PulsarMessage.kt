package com.zenaton.pulsar.workflows

import com.fasterxml.jackson.annotation.JsonInclude

@JsonInclude(JsonInclude.Include.NON_NULL)
class PulsarMessage() {
    var type: String? = null
    /**
     * Workflows
     */
    var workflowId: String? = null
    var workflowName: String? = null
    var workflowData: String? = null
    var workflowOutput: String? = null
    var parentWorkflowId: String? = null
    /**
     * Decisions
     */
    var decisionId: String? = null
    /**
     * Tasks
     */
    var taskId: String? = null
    var taskOutput: String? = null
    /**
     * Child Workflows
     */
    var childWorkflowId: String? = null
    var childWorkflowOutput: String? = null
    /**
     * Events
     */
    var eventName: String? = null
    var eventData: String? = null
    /**
     * Delays
     */
    var delayId: String? = null
    /**
     * Dates
     */
    var dispatchedAt: String? = null
    var receivedAt: String? = null
}
