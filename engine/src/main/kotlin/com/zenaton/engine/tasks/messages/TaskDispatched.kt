package com.zenaton.engine.tasks.messages

import com.zenaton.engine.interfaces.data.DateTime
import com.zenaton.engine.tasks.data.TaskData
import com.zenaton.engine.tasks.data.TaskId
import com.zenaton.engine.tasks.data.TaskName
import com.zenaton.engine.workflows.data.WorkflowId

data class TaskDispatched(
    override var taskId: TaskId,
    override var sentAt: DateTime? = DateTime(),
    override var receivedAt: DateTime? = null,
    val taskName: TaskName,
    val taskData: TaskData,
    val workflowId: WorkflowId? = null
) : TaskMessageInterface
