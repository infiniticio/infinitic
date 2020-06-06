package com.zenaton.jobManager.states

import com.zenaton.commons.data.interfaces.StateInterface
import com.zenaton.jobManager.data.JobName

data class MonitoringGlobalState(
    val jobNames: MutableSet<JobName> = mutableSetOf()
) : StateInterface
