package com.zenaton.workflowManager.messages.envelopes

import com.zenaton.commons.data.DateTime
import com.zenaton.workflowManager.data.WorkflowId

interface ForWorkflowEngineMessage {
    val workflowId: WorkflowId
    val sentAt: DateTime
}
