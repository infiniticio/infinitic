package com.zenaton.taskManager.monitoring.global

import com.zenaton.commons.data.interfaces.StateInterface
import com.zenaton.taskManager.data.TaskName

data class MonitoringGlobalState(
    val taskNames: MutableSet<TaskName> = mutableSetOf()
) : StateInterface
