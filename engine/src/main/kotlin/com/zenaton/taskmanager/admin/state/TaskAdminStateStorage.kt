package com.zenaton.taskmanager.admin.state

interface TaskAdminStateStorage {
    fun getState(): TaskAdminState?

    fun updateState(newState: TaskAdminState, oldState: TaskAdminState?)

    fun deleteState()
}
