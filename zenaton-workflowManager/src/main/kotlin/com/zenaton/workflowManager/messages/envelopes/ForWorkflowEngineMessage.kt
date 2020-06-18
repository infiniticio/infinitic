package com.zenaton.workflowManager.messages.envelopes

import com.zenaton.workflowManager.data.WorkflowId

interface ForWorkflowEngineMessage {
    val workflowId: WorkflowId
}
