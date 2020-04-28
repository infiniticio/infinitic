package com.zenaton.pulsar.topics.tasks.messages

import com.fasterxml.jackson.annotation.JsonInclude

@JsonInclude(JsonInclude.Include.NON_NULL)
class PulsarTaskMessage() {
    var type: String? = null
    /**
     * Tasks
     */
    var taskId: String? = null
    var taskName: String? = null
    var taskData: String? = null
    /**
     * Attempts
     */
    var taskAttemptId: String? = null
    var taskAttemptOutput: String? = null
    var taskAttemptError: String? = null
    /**
     * Workflows
     */
    var workflowId: String? = null
    /**
     * Dates
     */
    var dispatchedAt: String? = null
    var receivedAt: String? = null
}
