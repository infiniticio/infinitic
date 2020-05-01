package com.zenaton.engine.topics.workflows.messages

import com.zenaton.engine.data.DateTime
import com.zenaton.engine.data.workflows.WorkflowId
import com.zenaton.engine.data.workflows.WorkflowOutput

data class WorkflowCompleted(
    override var workflowId: WorkflowId,
    override var receivedAt: DateTime? = null,
    val workflowOutput: WorkflowOutput?,
    val dispatchedAt: DateTime
) : WorkflowMessageInterface
