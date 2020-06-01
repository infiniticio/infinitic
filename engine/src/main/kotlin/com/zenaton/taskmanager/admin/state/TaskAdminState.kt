package com.zenaton.taskmanager.admin.state

import com.zenaton.commons.data.interfaces.StateInterface
import com.zenaton.taskmanager.data.TaskName

data class TaskAdminState(
    val taskNames: MutableSet<TaskName> = mutableSetOf()
) : StateInterface
