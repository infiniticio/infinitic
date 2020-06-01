package com.zenaton.taskmanager.admin

import com.zenaton.taskmanager.admin.messages.TaskAdminMessage
import com.zenaton.taskmanager.admin.messages.TaskTypeCreated
import com.zenaton.taskmanager.admin.state.TaskAdminState
import com.zenaton.taskmanager.admin.state.TaskAdminStateStorage
import com.zenaton.taskmanager.dispatcher.TaskDispatcher
import com.zenaton.taskmanager.logger.TaskLogger

class TaskAdmin {
    lateinit var storage: TaskAdminStateStorage
    lateinit var dispatcher: TaskDispatcher
    lateinit var logger: TaskLogger

    fun handle(message: TaskAdminMessage) {
        // get associated state
        val oldState = storage.getState()
        val newState = oldState?.copy() ?: TaskAdminState()

        when (message) {
            is TaskTypeCreated -> handleTaskTypeCreated(message, newState)
        }

        // Update stored state if needed and existing
        if (newState != oldState) {
            storage.updateState(newState, oldState)
        }
    }

    private fun handleTaskTypeCreated(message: TaskTypeCreated, state: TaskAdminState) {
        val added = state.taskNames.add(message.taskName)

        if (!added) logger.warn("Trying to add a task %s already known in state %s", message.taskName, state)
    }
}
