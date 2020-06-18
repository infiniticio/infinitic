package com.zenaton.workflowManager.messages.envelopes

import com.zenaton.workflowManager.data.DecisionId

interface ForDecidersMessage {
    val decisionId: DecisionId
}
