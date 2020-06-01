package com.zenaton.taskmanager.engine.messages

import com.zenaton.commons.data.DateTime
import com.zenaton.taskmanager.data.TaskAttemptError
import com.zenaton.taskmanager.data.TaskAttemptId
import com.zenaton.taskmanager.data.TaskData
import com.zenaton.taskmanager.data.TaskId
import com.zenaton.taskmanager.data.TaskName
import com.zenaton.taskmanager.data.TaskOutput
import com.zenaton.taskmanager.messages.FailingTaskAttemptMessage
import com.zenaton.taskmanager.messages.TaskAttemptMessage
import com.zenaton.taskmanager.messages.TaskMessage
import com.zenaton.workflowengine.data.WorkflowId

sealed class TaskEngineMessage(
    override val taskId: TaskId,
    override val sentAt: DateTime
) : TaskMessage

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
) : TaskEngineMessage(taskId, sentAt), TaskAttemptMessage

data class TaskAttemptCompleted(
    override val taskId: TaskId,
    override val sentAt: DateTime = DateTime(),
    override val taskAttemptId: TaskAttemptId,
    override val taskAttemptIndex: Int,
    val taskOutput: TaskOutput?
) : TaskEngineMessage(taskId, sentAt), TaskAttemptMessage

data class TaskAttemptDispatched(
    override val taskId: TaskId,
    override val taskAttemptId: TaskAttemptId,
    override val taskAttemptIndex: Int,
    override val sentAt: DateTime = DateTime()
) : TaskEngineMessage(taskId, sentAt), TaskAttemptMessage

data class TaskAttemptFailed(
    override val taskId: TaskId,
    override val sentAt: DateTime = DateTime(),
    override val taskAttemptId: TaskAttemptId,
    override val taskAttemptIndex: Int,
    override val taskAttemptDelayBeforeRetry: Float?,
    val taskAttemptError: TaskAttemptError
) : TaskEngineMessage(taskId, sentAt), FailingTaskAttemptMessage

data class TaskAttemptStarted(
    override val taskId: TaskId,
    override val sentAt: DateTime = DateTime(),
    override val taskAttemptId: TaskAttemptId,
    override val taskAttemptIndex: Int
) : TaskEngineMessage(taskId, sentAt), TaskAttemptMessage

data class TaskCanceled(
    override val taskId: TaskId,
    override val sentAt: DateTime = DateTime()
) : TaskEngineMessage(taskId, sentAt), TaskMessage

data class TaskCompleted(
    override val taskId: TaskId,
    override val sentAt: DateTime = DateTime(),
    val taskOutput: TaskOutput?
) : TaskEngineMessage(taskId, sentAt), TaskMessage

data class TaskDispatched(
    override val taskId: TaskId,
    override val sentAt: DateTime = DateTime()
) : TaskEngineMessage(taskId, sentAt), TaskMessage
