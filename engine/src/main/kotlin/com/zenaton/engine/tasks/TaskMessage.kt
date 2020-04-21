package com.zenaton.engine.tasks

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.zenaton.engine.attributes.tasks.TaskAttemptError
import com.zenaton.engine.attributes.tasks.TaskAttemptId
import com.zenaton.engine.attributes.tasks.TaskData
import com.zenaton.engine.attributes.tasks.TaskId
import com.zenaton.engine.attributes.tasks.TaskName
import com.zenaton.engine.attributes.tasks.TaskOutput
import com.zenaton.engine.attributes.workflows.WorkflowId

sealed class TaskMessage(val type: String, open var taskId: TaskId) {
    fun getStateKey() = taskId.id
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class TaskDispatched(
    override var taskId: TaskId,
    val taskName: TaskName?,
    val taskData: TaskData?,
    val workflowId: WorkflowId?
) : TaskMessage("TaskDispatched", taskId)

@JsonIgnoreProperties(ignoreUnknown = true)
data class TaskCompleted(
    override var taskId: TaskId,
    val taskOutput: TaskOutput?
) : TaskMessage("TaskCompleted", taskId)

@JsonIgnoreProperties(ignoreUnknown = true)
data class TaskAttemptDispatched(
    override var taskId: TaskId,
    val taskName: TaskName?,
    val taskData: TaskData?,
    val taskAttemptId: TaskAttemptId
) : TaskMessage("TaskAttemptDispatched", taskId)

@JsonIgnoreProperties(ignoreUnknown = true)
data class TaskAttemptStarted(
    override var taskId: TaskId,
    val taskAttemptId: TaskAttemptId
) : TaskMessage("TaskAttemptStarted", taskId)

@JsonIgnoreProperties(ignoreUnknown = true)
data class TaskAttemptCompleted(
    override var taskId: TaskId,
    val taskAttemptId: TaskAttemptId,
    val taskOutput: TaskOutput?
) : TaskMessage("TaskAttemptCompleted", taskId)

@JsonIgnoreProperties(ignoreUnknown = true)
data class TaskAttemptFailed(
    override var taskId: TaskId,
    val taskAttemptId: TaskAttemptId,
    val taskAttemptError: TaskAttemptError
) : TaskMessage("TaskAttemptFailed", taskId)
