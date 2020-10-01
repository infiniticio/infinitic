// "Commons Clause" License Condition v1.0
//
// The Software is provided to you by the Licensor under the License, as defined
// below, subject to the following condition.
//
// Without limiting other conditions in the License, the grant of rights under the
// License will not include, and the License does not grant to you, the right to
// Sell the Software.
//
// For purposes of the foregoing, “Sell” means practicing any or all of the rights
// granted to you under the License to provide to third parties, for a fee or
// other consideration (including without limitation fees for hosting or
// consulting/ support services related to the Software), a product or service
// whose value derives, entirely or substantially, from the functionality of the
// Software. Any license notice or attribution required by the License must also
// include this Commons Clause License Condition notice.
//
// Software: Infinitic
//
// License: MIT License (https://opensource.org/licenses/MIT)
//
// Licensor: infinitic.io

package io.infinitic.common.tasks.messages

import io.infinitic.common.tasks.data.TaskAttemptError
import io.infinitic.common.tasks.data.TaskAttemptId
import io.infinitic.common.tasks.data.TaskAttemptIndex
import io.infinitic.common.tasks.data.TaskAttemptRetry
import io.infinitic.common.tasks.data.TaskId
import io.infinitic.common.tasks.data.TaskInput
import io.infinitic.common.tasks.data.TaskMeta
import io.infinitic.common.tasks.data.TaskName
import io.infinitic.common.tasks.data.TaskOptions
import io.infinitic.common.tasks.data.TaskOutput
import io.infinitic.common.tasks.data.TaskStatus
import io.infinitic.common.tasks.messages.interfaces.FailingTaskAttemptMessage
import io.infinitic.common.tasks.messages.interfaces.TaskAttemptMessage

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
