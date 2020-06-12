package com.zenaton.jobManager.messages.envelopes

import com.zenaton.commons.data.DateTime
import com.zenaton.jobManager.data.WorkflowId

interface ForWorkflowEngineMessage {
    val workflowId: WorkflowId
    val sentAt: DateTime
}
