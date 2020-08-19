package io.infinitic.taskManager.common.states

import io.infinitic.taskManager.common.avro.AvroConverter
import io.infinitic.taskManager.common.data.TaskAttemptId
import io.infinitic.taskManager.common.data.TaskAttemptIndex
import io.infinitic.taskManager.common.data.TaskAttemptRetry
import io.infinitic.taskManager.common.data.TaskId
import io.infinitic.taskManager.common.data.TaskInput
import io.infinitic.taskManager.common.data.TaskMeta
import io.infinitic.taskManager.common.data.TaskName
import io.infinitic.taskManager.common.data.TaskOptions
import io.infinitic.taskManager.common.data.TaskStatus

sealed class State

data class TaskEngineState(
    val taskId: TaskId,
    val taskName: TaskName,
    val taskStatus: TaskStatus,
    val taskInput: TaskInput,
    var taskAttemptId: TaskAttemptId,
    var taskAttemptIndex: TaskAttemptIndex = TaskAttemptIndex(0),
    var taskAttemptRetry: TaskAttemptRetry = TaskAttemptRetry(0),
    val taskOptions: TaskOptions,
    val taskMeta: TaskMeta
) : State() {
    fun deepCopy() = AvroConverter.fromStorage(AvroConverter.toStorage(this))
}

data class MonitoringPerNameState(
    val taskName: TaskName,
    var runningOkCount: Long = 0,
    var runningWarningCount: Long = 0,
    var runningErrorCount: Long = 0,
    var terminatedCompletedCount: Long = 0,
    var terminatedCanceledCount: Long = 0
) : State() {
    fun deepCopy() = AvroConverter.fromStorage(AvroConverter.toStorage(this))
}

data class MonitoringGlobalState(
    val taskNames: MutableSet<TaskName> = mutableSetOf()
) : State() {
    fun deepCopy() = AvroConverter.fromStorage(AvroConverter.toStorage(this))
}
