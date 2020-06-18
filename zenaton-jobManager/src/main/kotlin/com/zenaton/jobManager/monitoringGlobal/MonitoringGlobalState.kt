package com.zenaton.jobManager.monitoringGlobal

import com.zenaton.common.data.interfaces.StateInterface
import com.zenaton.jobManager.data.JobName

data class MonitoringGlobalState(
    val jobNames: MutableSet<JobName> = mutableSetOf()
) : StateInterface
