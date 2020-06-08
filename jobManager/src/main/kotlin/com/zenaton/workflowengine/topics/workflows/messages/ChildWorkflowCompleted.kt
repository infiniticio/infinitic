package com.zenaton.workflowengine.topics.workflows.messages

import com.zenaton.commons.data.DateTime
import com.zenaton.workflowengine.data.WorkflowId
import com.zenaton.workflowengine.data.WorkflowOutput
import com.zenaton.workflowengine.topics.workflows.interfaces.WorkflowMessageInterface

data class ChildWorkflowCompleted(
    override var workflowId: WorkflowId,
    override var sentAt: DateTime? = DateTime(),
    val childWorkflowId: WorkflowId,
    val childWorkflowOutput: WorkflowOutput?
) : WorkflowMessageInterface
