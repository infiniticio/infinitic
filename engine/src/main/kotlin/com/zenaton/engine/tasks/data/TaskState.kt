package com.zenaton.engine.tasks.data

import com.zenaton.engine.interfaces.data.StateInterface
import com.zenaton.engine.taskAttempts.data.TaskAttemptId
import com.zenaton.engine.workflows.data.WorkflowId

data class TaskState(
    val taskId: TaskId,
    val taskName: TaskName,
    val taskData: TaskData,
    var taskAttemptId: TaskAttemptId = TaskAttemptId(),
    var taskAttemptIndex: Int = 0,
    val workflowId: WorkflowId? = null
) : StateInterface
