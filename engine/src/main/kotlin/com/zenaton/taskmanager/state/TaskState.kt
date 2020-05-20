package com.zenaton.taskmanager.state

import com.zenaton.commons.data.interfaces.StateInterface
import com.zenaton.taskmanager.data.TaskAttemptId
import com.zenaton.taskmanager.data.TaskData
import com.zenaton.taskmanager.data.TaskId
import com.zenaton.taskmanager.data.TaskName
import com.zenaton.taskmanager.data.TaskStatus
import com.zenaton.workflowengine.data.WorkflowId

data class TaskState(
    val taskId: TaskId,
    val taskName: TaskName,
    var taskStatus: TaskStatus,
    val taskData: TaskData?,
    var taskAttemptId: TaskAttemptId,
    var taskAttemptIndex: Int,
    val workflowId: WorkflowId? = null
) : StateInterface
