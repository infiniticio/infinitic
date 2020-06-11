package com.zenaton.workflowManager.messages.envelopes

import com.zenaton.commons.data.DateTime
import com.zenaton.workflowManager.data.DecisionId
import com.zenaton.workflowManager.data.WorkflowId

interface ForDecidersMessage {
    val decisionId: DecisionId
    val sentAt: DateTime
}
