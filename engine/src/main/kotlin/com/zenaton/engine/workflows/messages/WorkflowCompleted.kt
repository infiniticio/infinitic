package com.zenaton.engine.workflows.messages

import com.zenaton.engine.interfaces.data.DateTime
import com.zenaton.engine.workflows.data.WorkflowId
import com.zenaton.engine.workflows.data.WorkflowOutput

data class WorkflowCompleted(
    override var workflowId: WorkflowId,
    override var sentAt: DateTime? = DateTime(),
    override var receivedAt: DateTime? = null,
    val workflowOutput: WorkflowOutput?,
    val dispatchedAt: DateTime
) : WorkflowMessageInterface
