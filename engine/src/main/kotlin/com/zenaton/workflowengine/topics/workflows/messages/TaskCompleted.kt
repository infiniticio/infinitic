package com.zenaton.workflowengine.topics.workflows.messages

import com.zenaton.commons.data.DateTime
import com.zenaton.taskManager.data.TaskId
import com.zenaton.taskManager.data.TaskOutput
import com.zenaton.workflowengine.data.WorkflowId
import com.zenaton.workflowengine.topics.workflows.interfaces.WorkflowMessageInterface

data class TaskCompleted(
    override var workflowId: WorkflowId,
    override var sentAt: DateTime? = DateTime(),
    val taskId: TaskId,
    val taskOutput: TaskOutput?
) : WorkflowMessageInterface
