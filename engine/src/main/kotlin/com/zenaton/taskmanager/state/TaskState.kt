package com.zenaton.taskmanager.state

import com.zenaton.commons.data.interfaces.StateInterface
import com.zenaton.taskmanager.data.TaskAttemptId
import com.zenaton.taskmanager.data.TaskData
import com.zenaton.taskmanager.data.TaskId
import com.zenaton.taskmanager.data.TaskName
import com.zenaton.workflowengine.data.WorkflowId

data class TaskState(
    val taskId: TaskId,
    val taskName: TaskName,
    val taskData: TaskData? = null,
    var taskAttemptId: TaskAttemptId = TaskAttemptId(),
    var taskAttemptIndex: Int = 0,
    val workflowId: WorkflowId? = null
) : StateInterface
