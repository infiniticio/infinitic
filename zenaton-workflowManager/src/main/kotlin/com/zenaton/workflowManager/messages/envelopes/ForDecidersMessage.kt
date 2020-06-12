package com.zenaton.workflowManager.messages.envelopes

import com.zenaton.commons.data.DateTime
import com.zenaton.workflowManager.data.DecisionId

interface ForDecidersMessage {
    val decisionId: DecisionId
    val sentAt: DateTime
}
