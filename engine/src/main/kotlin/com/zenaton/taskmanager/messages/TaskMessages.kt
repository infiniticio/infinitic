package com.zenaton.taskmanager.messages

import com.fasterxml.jackson.annotation.JsonIgnore
import com.zenaton.commons.data.DateTime
import com.zenaton.taskmanager.data.TaskAttemptError
import com.zenaton.taskmanager.data.TaskAttemptId
import com.zenaton.taskmanager.data.TaskData
import com.zenaton.taskmanager.data.TaskId
import com.zenaton.taskmanager.data.TaskName
import com.zenaton.taskmanager.data.TaskOutput
import com.zenaton.taskmanager.data.TaskStatus
import com.zenaton.workflowengine.data.WorkflowId

sealed class TaskMessage(
    open val taskId: TaskId,
    open val sentAt: DateTime
) { @JsonIgnore fun getStateId() = taskId.id }

sealed class TaskAttemptMessage(
    override val taskId: TaskId,
    override val sentAt: DateTime,
    open val taskAttemptId: TaskAttemptId,
    open val taskAttemptIndex: Int
) : TaskMessage(taskId, sentAt)

sealed class FailingTaskAttemptMessage(
    override val taskId: TaskId,
    override val sentAt: DateTime,
    override val taskAttemptId: TaskAttemptId,
    override val taskAttemptIndex: Int,
    open val taskAttemptDelayBeforeRetry: Float?
) : TaskAttemptMessage(taskId, sentAt, taskAttemptId, taskAttemptIndex)

data class CancelTask(
    override val taskId: TaskId,
    override val sentAt: DateTime = DateTime()
) : TaskMessage(taskId, sentAt)

data class DispatchTask(
    override val taskId: TaskId,
    override val sentAt: DateTime = DateTime(),
    val taskName: TaskName,
    val taskData: TaskData?,
    val workflowId: WorkflowId? = null
) : TaskMessage(taskId, sentAt)

data class RetryTask(
    override val taskId: TaskId,
    override val sentAt: DateTime = DateTime()
) : TaskMessage(taskId, sentAt)

data class RetryTaskAttempt(
    override val taskId: TaskId,
    override val sentAt: DateTime = DateTime(),
    override val taskAttemptId: TaskAttemptId,
    override val taskAttemptIndex: Int
) : TaskAttemptMessage(taskId, sentAt, taskAttemptId, taskAttemptIndex)

data class RunTask(
    override val taskId: TaskId,
    override val sentAt: DateTime = DateTime(),
    override val taskAttemptId: TaskAttemptId,
    override val taskAttemptIndex: Int,
    val taskName: TaskName,
    val taskData: TaskData?
) : TaskAttemptMessage(taskId, sentAt, taskAttemptId, taskAttemptIndex)

data class TaskAttemptCompleted(
    override val taskId: TaskId,
    override val sentAt: DateTime = DateTime(),
    override val taskAttemptId: TaskAttemptId,
    override val taskAttemptIndex: Int,
    val taskOutput: TaskOutput?
) : TaskAttemptMessage(taskId, sentAt, taskAttemptId, taskAttemptIndex)

data class TaskAttemptDispatched(
    override val taskId: TaskId,
    override val taskAttemptId: TaskAttemptId,
    override val taskAttemptIndex: Int,
    override val sentAt: DateTime = DateTime()
) : TaskAttemptMessage(taskId, sentAt, taskAttemptId, taskAttemptIndex)

data class TaskAttemptFailed(
    override val taskId: TaskId,
    override val sentAt: DateTime = DateTime(),
    override val taskAttemptId: TaskAttemptId,
    override val taskAttemptIndex: Int,
    override val taskAttemptDelayBeforeRetry: Float?,
    val taskAttemptError: TaskAttemptError
) : FailingTaskAttemptMessage(taskId, sentAt, taskAttemptId, taskAttemptIndex, taskAttemptDelayBeforeRetry)

data class TaskAttemptStarted(
    override val taskId: TaskId,
    override val sentAt: DateTime = DateTime(),
    override val taskAttemptId: TaskAttemptId,
    override val taskAttemptIndex: Int
) : TaskAttemptMessage(taskId, sentAt, taskAttemptId, taskAttemptIndex)

data class TaskCanceled(
    override val taskId: TaskId,
    override val sentAt: DateTime = DateTime()
) : TaskMessage(taskId, sentAt)

data class TaskCompleted(
    override val taskId: TaskId,
    override val sentAt: DateTime = DateTime(),
    val taskOutput: TaskOutput?
) : TaskMessage(taskId, sentAt)

data class TaskDispatched(
    override val taskId: TaskId,
    override val sentAt: DateTime = DateTime()
) : TaskMessage(taskId, sentAt)

data class TaskStatusUpdated(
    override var taskId: TaskId,
    override var sentAt: DateTime = DateTime(),
    var taskName: TaskName,
    val oldStatus: TaskStatus?,
    val newStatus: TaskStatus?
) : TaskMessage(taskId, sentAt)
