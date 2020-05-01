package com.zenaton.engine.topics.workflows.messages

import com.zenaton.engine.data.DateTime
import com.zenaton.engine.data.tasks.TaskId
import com.zenaton.engine.data.tasks.TaskOutput
import com.zenaton.engine.data.workflows.WorkflowId

data class TaskCompleted(
    override var workflowId: WorkflowId,
    override var receivedAt: DateTime? = null,
    val taskId: TaskId,
    val taskOutput: TaskOutput?
) : WorkflowMessageInterface
