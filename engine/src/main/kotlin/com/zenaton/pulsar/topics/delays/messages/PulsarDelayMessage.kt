package com.zenaton.pulsar.topics.delays.messages

import com.fasterxml.jackson.annotation.JsonInclude

@JsonInclude(JsonInclude.Include.NON_NULL)
class PulsarDelayMessage() {
    var type: String? = null
    /**
     * Delay
     */
    var delayId: String? = null
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
