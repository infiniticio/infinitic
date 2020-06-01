package com.zenaton.taskManager.monitoring.global

import com.zenaton.commons.data.interfaces.StateInterface
import com.zenaton.taskManager.data.JobName

data class MonitoringGlobalState(
    val jobNames: MutableSet<JobName> = mutableSetOf()
) : StateInterface
