package com.zenaton.engine.topics.tasks.state

import com.zenaton.engine.data.TaskAttemptId
import com.zenaton.engine.data.TaskData
import com.zenaton.engine.data.TaskId
import com.zenaton.engine.data.TaskName
import com.zenaton.engine.data.WorkflowId
import com.zenaton.engine.data.interfaces.StateInterface

data class TaskState(
    val taskId: TaskId,
    val taskName: TaskName,
    val taskData: TaskData?,
    var taskAttemptId: TaskAttemptId = TaskAttemptId(),
    var taskAttemptIndex: Int = 0,
    val workflowId: WorkflowId? = null
) : StateInterface
