package com.zenaton.taskManager.common.states

import com.zenaton.taskManager.common.avro.AvroConverter
import com.zenaton.taskManager.common.data.TaskAttemptId
import com.zenaton.taskManager.common.data.TaskAttemptIndex
import com.zenaton.taskManager.common.data.TaskAttemptRetry
import com.zenaton.taskManager.common.data.TaskId
import com.zenaton.taskManager.common.data.TaskInput
import com.zenaton.taskManager.common.data.TaskMeta
import com.zenaton.taskManager.common.data.TaskName
import com.zenaton.taskManager.common.data.TaskOptions
import com.zenaton.taskManager.common.data.TaskStatus

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
