package com.zenaton.engine.topics.tasks.messages

import com.zenaton.engine.data.DateTime
import com.zenaton.engine.data.TaskData
import com.zenaton.engine.data.TaskId
import com.zenaton.engine.data.TaskName
import com.zenaton.engine.data.WorkflowId
import com.zenaton.engine.topics.tasks.interfaces.TaskMessageInterface

data class TaskDispatched(
    override var taskId: TaskId,
    override var sentAt: DateTime? = DateTime(),
    val taskName: TaskName,
    val taskData: TaskData,
    val workflowId: WorkflowId? = null
) : TaskMessageInterface
