package com.zenaton.taskmanager.engine.state

import com.zenaton.commons.data.interfaces.StateInterface
import com.zenaton.taskmanager.data.*
import com.zenaton.workflowengine.data.WorkflowId

data class TaskEngineState(
    val taskId: TaskId,
    val taskName: TaskName,
    var taskStatus: TaskStatus,
    val taskData: TaskData?,
    var taskAttemptId: TaskAttemptId,
    var taskAttemptIndex: Int,
    val workflowId: WorkflowId? = null
) : StateInterface
