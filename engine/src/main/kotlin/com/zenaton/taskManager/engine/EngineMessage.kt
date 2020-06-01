package com.zenaton.taskManager.engine

import com.zenaton.commons.data.DateTime
import com.zenaton.taskManager.data.TaskAttemptError
import com.zenaton.taskManager.data.TaskAttemptId
import com.zenaton.taskManager.data.TaskData
import com.zenaton.taskManager.data.TaskId
import com.zenaton.taskManager.data.TaskName
import com.zenaton.taskManager.data.TaskOutput
import com.zenaton.taskManager.messages.FailingTaskAttemptMessage
import com.zenaton.taskManager.messages.TaskAttemptMessage
import com.zenaton.taskManager.messages.TaskMessage
import com.zenaton.workflowengine.data.WorkflowId

sealed class EngineMessage(
    override val taskId: TaskId,
    override val sentAt: DateTime
) : TaskMessage

data class CancelTask(
    override val taskId: TaskId,
    override val sentAt: DateTime = DateTime()
) : EngineMessage(taskId, sentAt)

data class DispatchTask(
    override val taskId: TaskId,
    override val sentAt: DateTime = DateTime(),
    val taskName: TaskName,
    val taskData: TaskData?,
    val workflowId: WorkflowId? = null
) : EngineMessage(taskId, sentAt)

data class RetryTask(
    override val taskId: TaskId,
    override val sentAt: DateTime = DateTime()
) : EngineMessage(taskId, sentAt)

data class RetryTaskAttempt(
    override val taskId: TaskId,
    override val sentAt: DateTime = DateTime(),
    override val taskAttemptId: TaskAttemptId,
    override val taskAttemptIndex: Int
) : EngineMessage(taskId, sentAt), TaskAttemptMessage

data class TaskAttemptCompleted(
    override val taskId: TaskId,
    override val sentAt: DateTime = DateTime(),
    override val taskAttemptId: TaskAttemptId,
    override val taskAttemptIndex: Int,
    val taskOutput: TaskOutput?
) : EngineMessage(taskId, sentAt), TaskAttemptMessage

data class TaskAttemptDispatched(
    override val taskId: TaskId,
    override val taskAttemptId: TaskAttemptId,
    override val taskAttemptIndex: Int,
    override val sentAt: DateTime = DateTime()
) : EngineMessage(taskId, sentAt), TaskAttemptMessage

data class TaskAttemptFailed(
    override val taskId: TaskId,
    override val sentAt: DateTime = DateTime(),
    override val taskAttemptId: TaskAttemptId,
    override val taskAttemptIndex: Int,
    override val taskAttemptDelayBeforeRetry: Float?,
    val taskAttemptError: TaskAttemptError
) : EngineMessage(taskId, sentAt), FailingTaskAttemptMessage

data class TaskAttemptStarted(
    override val taskId: TaskId,
    override val sentAt: DateTime = DateTime(),
    override val taskAttemptId: TaskAttemptId,
    override val taskAttemptIndex: Int
) : EngineMessage(taskId, sentAt), TaskAttemptMessage

data class TaskCanceled(
    override val taskId: TaskId,
    override val sentAt: DateTime = DateTime()
) : EngineMessage(taskId, sentAt), TaskMessage

data class TaskCompleted(
    override val taskId: TaskId,
    override val sentAt: DateTime = DateTime(),
    val taskOutput: TaskOutput?
) : EngineMessage(taskId, sentAt), TaskMessage

data class TaskDispatched(
    override val taskId: TaskId,
    override val sentAt: DateTime = DateTime()
) : EngineMessage(taskId, sentAt), TaskMessage
