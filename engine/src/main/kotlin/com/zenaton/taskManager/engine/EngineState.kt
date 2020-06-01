package com.zenaton.taskManager.engine

import com.zenaton.commons.data.interfaces.StateInterface
import com.zenaton.taskManager.data.TaskAttemptId
import com.zenaton.taskManager.data.TaskData
import com.zenaton.taskManager.data.TaskId
import com.zenaton.taskManager.data.TaskName
import com.zenaton.taskManager.data.TaskStatus
import com.zenaton.workflowengine.data.WorkflowId

data class EngineState(
    val taskId: TaskId,
    val taskName: TaskName,
    var taskStatus: TaskStatus,
    val taskData: TaskData?,
    var taskAttemptId: TaskAttemptId,
    var taskAttemptIndex: Int,
    val workflowId: WorkflowId? = null
) : StateInterface
