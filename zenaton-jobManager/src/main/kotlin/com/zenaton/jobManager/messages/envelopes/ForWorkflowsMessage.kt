package com.zenaton.jobManager.messages.envelopes

import com.zenaton.commons.data.DateTime
import com.zenaton.jobManager.data.WorkflowId

interface ForWorkflowsMessage {
    val workflowId: WorkflowId
    val sentAt: DateTime
}
