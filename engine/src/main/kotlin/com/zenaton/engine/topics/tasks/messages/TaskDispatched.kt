package com.zenaton.engine.topics.tasks.messages

import com.zenaton.engine.data.DateTime
import com.zenaton.engine.data.tasks.TaskData
import com.zenaton.engine.data.tasks.TaskId
import com.zenaton.engine.data.tasks.TaskName
import com.zenaton.engine.data.workflows.WorkflowId

data class TaskDispatched(
    override var taskId: TaskId,
    override var receivedAt: DateTime? = null,
    val taskName: TaskName?,
    val taskData: TaskData?,
    val workflowId: WorkflowId?
) : TaskMessageInterface
