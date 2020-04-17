package com.zenaton.pulsar.workflows

class PulsarMessage() {
    var type: String? = null
    var workflowId: String? = null
    var workflowName: String? = null
    var workflowData: String? = null
    var decisionId: String? = null
    var taskId: String? = null
    var delayId: String? = null
}
