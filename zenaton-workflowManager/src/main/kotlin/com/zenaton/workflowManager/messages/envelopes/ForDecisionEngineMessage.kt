package com.zenaton.workflowManager.messages.envelopes

import com.zenaton.workflowManager.data.DecisionId

interface ForDecisionEngineMessage {
    val decisionId: DecisionId
}
