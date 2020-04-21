package com.zenaton.pulsar.workflows

import com.fasterxml.jackson.annotation.JsonInclude

@JsonInclude(JsonInclude.Include.NON_NULL)
class PulsarMessage() {
    var type: String? = null
    var workflowId: String? = null
    var workflowName: String? = null
    var workflowData: String? = null
    var workflowOutput: String? = null
    var decisionId: String? = null
    var taskId: String? = null
    var taskOutput: String? = null
    var delayId: String? = null
    var dispatchedAt: String? = null
}
