package com.zenaton.taskmanager.messages.engine

import com.fasterxml.jackson.annotation.JsonIgnore
import com.zenaton.commons.data.DateTime
import com.zenaton.taskmanager.data.TaskAttemptError
import com.zenaton.taskmanager.data.TaskAttemptId
import com.zenaton.taskmanager.data.TaskData
import com.zenaton.taskmanager.data.TaskId
import com.zenaton.taskmanager.data.TaskName
import com.zenaton.taskmanager.data.TaskOutput
import com.zenaton.taskmanager.messages.interfaces.FailingTaskAttemptMessageInterface
import com.zenaton.taskmanager.messages.interfaces.TaskAttemptMessageInterface
import com.zenaton.taskmanager.messages.interfaces.TaskMessageInterface
import com.zenaton.workflowengine.data.WorkflowId

sealed class TaskEngineMessage(
    override val taskId: TaskId,
    override val sentAt: DateTime
) : TaskMessageInterface {
    @JsonIgnore fun getStateId() = taskId.id
}

data class CancelTask(
    override val taskId: TaskId,
    override val sentAt: DateTime = DateTime()
) : TaskEngineMessage(taskId, sentAt)

data class DispatchTask(
    override val taskId: TaskId,
    override val sentAt: DateTime = DateTime(),
    val taskName: TaskName,
    val taskData: TaskData?,
    val workflowId: WorkflowId? = null
) : TaskEngineMessage(taskId, sentAt)

data class RetryTask(
    override val taskId: TaskId,
    override val sentAt: DateTime = DateTime()
) : TaskEngineMessage(taskId, sentAt)

data class RetryTaskAttempt(
    override val taskId: TaskId,
    override val sentAt: DateTime = DateTime(),
    override val taskAttemptId: TaskAttemptId,
    override val taskAttemptIndex: Int
) : TaskEngineMessage(taskId, sentAt), TaskAttemptMessageInterface

data class TaskAttemptCompleted(
    override val taskId: TaskId,
    override val sentAt: DateTime = DateTime(),
    override val taskAttemptId: TaskAttemptId,
    override val taskAttemptIndex: Int,
    val taskOutput: TaskOutput?
) : TaskEngineMessage(taskId, sentAt), TaskAttemptMessageInterface

data class TaskAttemptDispatched(
    override val taskId: TaskId,
    override val taskAttemptId: TaskAttemptId,
    override val taskAttemptIndex: Int,
    override val sentAt: DateTime = DateTime()
) : TaskEngineMessage(taskId, sentAt), TaskAttemptMessageInterface

data class TaskAttemptFailed(
    override val taskId: TaskId,
    override val sentAt: DateTime = DateTime(),
    override val taskAttemptId: TaskAttemptId,
    override val taskAttemptIndex: Int,
    override val taskAttemptDelayBeforeRetry: Float?,
    val taskAttemptError: TaskAttemptError
) : TaskEngineMessage(taskId, sentAt), FailingTaskAttemptMessageInterface

data class TaskAttemptStarted(
    override val taskId: TaskId,
    override val sentAt: DateTime = DateTime(),
    override val taskAttemptId: TaskAttemptId,
    override val taskAttemptIndex: Int
) : TaskEngineMessage(taskId, sentAt), TaskAttemptMessageInterface

data class TaskCanceled(
    override val taskId: TaskId,
    override val sentAt: DateTime = DateTime()
) : TaskEngineMessage(taskId, sentAt), TaskMessageInterface

data class TaskCompleted(
    override val taskId: TaskId,
    override val sentAt: DateTime = DateTime(),
    val taskOutput: TaskOutput?
) : TaskEngineMessage(taskId, sentAt), TaskMessageInterface

data class TaskDispatched(
    override val taskId: TaskId,
    override val sentAt: DateTime = DateTime()
) : TaskEngineMessage(taskId, sentAt), TaskMessageInterface
