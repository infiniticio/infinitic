package com.zenaton.taskmanager.metrics.state

import com.zenaton.commons.data.interfaces.StateInterface
import com.zenaton.taskmanager.data.TaskName

data class TaskMetricsState(
    val taskName: TaskName,
    var okCount: Long = 0,
    var warningCount: Long = 0,
    var errorCount: Long = 0,
    var terminatedCompletedCount: Long = 0,
    var terminatedCanceledCount: Long = 0
) : StateInterface
