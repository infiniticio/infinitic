package com.zenaton.taskManager.monitoring.perName

import com.zenaton.commons.data.interfaces.StateInterface
import com.zenaton.taskManager.data.JobName

data class MonitoringPerNameState(
    val jobName: JobName,
    var runningOkCount: Long = 0,
    var runningWarningCount: Long = 0,
    var runningErrorCount: Long = 0,
    var terminatedCompletedCount: Long = 0,
    var terminatedCanceledCount: Long = 0
) : StateInterface
