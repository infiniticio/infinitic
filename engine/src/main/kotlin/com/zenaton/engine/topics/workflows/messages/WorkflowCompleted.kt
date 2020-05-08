package com.zenaton.engine.topics.workflows.messages

import com.zenaton.engine.data.DateTime
import com.zenaton.engine.data.WorkflowId
import com.zenaton.engine.data.WorkflowOutput
import com.zenaton.engine.topics.workflows.interfaces.WorkflowMessageInterface

data class WorkflowCompleted(
    override var workflowId: WorkflowId,
    override var sentAt: DateTime? = DateTime(),
    val workflowOutput: WorkflowOutput?,
    val dispatchedAt: DateTime
) : WorkflowMessageInterface
