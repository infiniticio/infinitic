package com.zenaton.engine.workflows.messages

import com.zenaton.engine.interfaces.data.DateTime
import com.zenaton.engine.tasks.data.TaskId
import com.zenaton.engine.tasks.data.TaskOutput
import com.zenaton.engine.workflows.data.WorkflowId

data class TaskCompleted(
    override var workflowId: WorkflowId,
    override var sentAt: DateTime? = DateTime(),
    override var receivedAt: DateTime? = null,
    val taskId: TaskId,
    val taskOutput: TaskOutput?
) : WorkflowMessageInterface
