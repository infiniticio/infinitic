package com.zenaton.engine.topics.workflows.messages

import com.zenaton.engine.data.DateTime
import com.zenaton.engine.data.WorkflowId
import com.zenaton.engine.topics.workflows.interfaces.WorkflowMessageInterface
import com.zenaton.taskmanager.data.TaskId
import com.zenaton.taskmanager.data.TaskOutput

data class TaskCompleted(
    override var workflowId: WorkflowId,
    override var sentAt: DateTime? = DateTime(),
    val taskId: TaskId,
    val taskOutput: TaskOutput?
) : WorkflowMessageInterface
