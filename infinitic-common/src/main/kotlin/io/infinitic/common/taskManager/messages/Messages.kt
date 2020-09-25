package io.infinitic.common.taskManager.messages

import io.infinitic.common.taskManager.data.TaskAttemptError
import io.infinitic.common.taskManager.data.TaskAttemptId
import io.infinitic.common.taskManager.data.TaskAttemptIndex
import io.infinitic.common.taskManager.data.TaskAttemptRetry
import io.infinitic.common.taskManager.data.TaskId
import io.infinitic.common.taskManager.data.TaskInput
import io.infinitic.common.taskManager.data.TaskMeta
import io.infinitic.common.taskManager.data.TaskName
import io.infinitic.common.taskManager.data.TaskOptions
import io.infinitic.common.taskManager.data.TaskOutput
import io.infinitic.common.taskManager.data.TaskStatus
import io.infinitic.common.taskManager.messages.interfaces.FailingTaskAttemptMessage
import io.infinitic.common.taskManager.messages.interfaces.TaskAttemptMessage

sealed class Message

sealed class ForTaskEngineMessage(open val taskId: TaskId) : Message()

sealed class ForMonitoringPerNameMessage(open val taskName: TaskName) : Message()

sealed class ForMonitoringGlobalMessage : Message()

sealed class ForWorkerMessage(open val taskName: TaskName) : Message()

/*
 * Task Engine Messages
 */

data class CancelTask(
    override val taskId: TaskId,
    val taskOutput: TaskOutput
) : ForTaskEngineMessage(taskId)

data class DispatchTask(
    override val taskId: TaskId,
    val taskName: TaskName,
    val taskInput: TaskInput,
    val taskMeta: TaskMeta,
    val taskOptions: TaskOptions = TaskOptions()
) : ForTaskEngineMessage(taskId)

data class TaskAttemptCompleted(
    override val taskId: TaskId,
    override val taskAttemptId: TaskAttemptId,
    override val taskAttemptRetry: TaskAttemptRetry,
    override val taskAttemptIndex: TaskAttemptIndex,
    val taskOutput: TaskOutput
) : ForTaskEngineMessage(taskId), TaskAttemptMessage

data class TaskAttemptDispatched(
    override val taskId: TaskId,
    override val taskAttemptId: TaskAttemptId,
    override val taskAttemptRetry: TaskAttemptRetry,
    override val taskAttemptIndex: TaskAttemptIndex
) : ForTaskEngineMessage(taskId), TaskAttemptMessage

data class TaskAttemptFailed(
    override val taskId: TaskId,
    override val taskAttemptId: TaskAttemptId,
    override val taskAttemptRetry: TaskAttemptRetry,
    override val taskAttemptIndex: TaskAttemptIndex,
    override val taskAttemptDelayBeforeRetry: Float?,
    val taskAttemptError: TaskAttemptError
) : ForTaskEngineMessage(taskId), FailingTaskAttemptMessage

data class TaskAttemptStarted(
    override val taskId: TaskId,
    override val taskAttemptId: TaskAttemptId,
    override val taskAttemptRetry: TaskAttemptRetry,
    override val taskAttemptIndex: TaskAttemptIndex
) : ForTaskEngineMessage(taskId), TaskAttemptMessage

data class TaskCanceled(
    override val taskId: TaskId,
    val taskOutput: TaskOutput,
    val taskMeta: TaskMeta
) : ForTaskEngineMessage(taskId)

data class TaskCompleted(
    override val taskId: TaskId,
    val taskName: TaskName,
    val taskOutput: TaskOutput,
    val taskMeta: TaskMeta
) : ForTaskEngineMessage(taskId)

data class RetryTask(
    override val taskId: TaskId,
    val taskName: TaskName?,
    val taskInput: TaskInput?,
    val taskMeta: TaskMeta?,
    val taskOptions: TaskOptions?
) : ForTaskEngineMessage(taskId)

data class RetryTaskAttempt(
    override val taskId: TaskId,
    override val taskAttemptId: TaskAttemptId,
    override val taskAttemptRetry: TaskAttemptRetry,
    override val taskAttemptIndex: TaskAttemptIndex
) : ForTaskEngineMessage(taskId), TaskAttemptMessage

/*
 * Monitoring Per Name Messages
 */

data class TaskStatusUpdated constructor(
    override val taskName: TaskName,
    val taskId: TaskId,
    val oldStatus: TaskStatus?,
    val newStatus: TaskStatus
) : ForMonitoringPerNameMessage(taskName)

/*
 * Monitoring Global Messages
 */

data class TaskCreated(
    val taskName: TaskName
) : ForMonitoringGlobalMessage()

/*
 * Worker Messages
 */

data class RunTask(
    override val taskName: TaskName,
    override val taskId: TaskId,
    override val taskAttemptId: TaskAttemptId,
    override val taskAttemptRetry: TaskAttemptRetry,
    override val taskAttemptIndex: TaskAttemptIndex,
    val taskInput: TaskInput,
    val taskOptions: TaskOptions,
    val taskMeta: TaskMeta
) : ForWorkerMessage(taskName), TaskAttemptMessage
