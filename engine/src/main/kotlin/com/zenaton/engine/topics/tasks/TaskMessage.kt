package com.zenaton.engine.topics.tasks

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.zenaton.engine.data.tasks.TaskAttemptError
import com.zenaton.engine.data.tasks.TaskAttemptId
import com.zenaton.engine.data.tasks.TaskData
import com.zenaton.engine.data.tasks.TaskId
import com.zenaton.engine.data.tasks.TaskName
import com.zenaton.engine.data.tasks.TaskOutput
import com.zenaton.engine.data.workflows.WorkflowId

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes(
    JsonSubTypes.Type(value = TaskDispatched::class, name = "TaskDispatched"),
    JsonSubTypes.Type(value = TaskCompleted::class, name = "TaskCompleted"),
    JsonSubTypes.Type(value = TaskAttemptDispatched::class, name = "TaskAttemptDispatched"),
    JsonSubTypes.Type(value = TaskAttemptStarted::class, name = "TaskAttemptStarted"),
    JsonSubTypes.Type(value = TaskAttemptCompleted::class, name = "TaskAttemptCompleted"),
    JsonSubTypes.Type(value = TaskAttemptFailed::class, name = "TaskAttemptFailed")
)
sealed class TaskMessage(open var taskId: TaskId) {
    @JsonIgnore
    fun getStateKey() = taskId.id
}

data class TaskDispatched(
    override var taskId: TaskId,
    val taskName: TaskName?,
    val taskData: TaskData?,
    val workflowId: WorkflowId?
) : TaskMessage(taskId)

data class TaskCompleted(
    override var taskId: TaskId,
    val taskOutput: TaskOutput?
) : TaskMessage(taskId)

data class TaskAttemptDispatched(
    override var taskId: TaskId,
    val taskName: TaskName?,
    val taskData: TaskData?,
    val taskAttemptId: TaskAttemptId
) : TaskMessage(taskId)

data class TaskAttemptStarted(
    override var taskId: TaskId,
    val taskAttemptId: TaskAttemptId
) : TaskMessage(taskId)

data class TaskAttemptCompleted(
    override var taskId: TaskId,
    val taskAttemptId: TaskAttemptId,
    val taskOutput: TaskOutput?
) : TaskMessage(taskId)

data class TaskAttemptFailed(
    override var taskId: TaskId,
    val taskAttemptId: TaskAttemptId,
    val taskAttemptError: TaskAttemptError
) : TaskMessage(taskId)
