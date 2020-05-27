package com.zenaton.taskmanager.state

import com.zenaton.taskmanager.data.TaskId
import com.zenaton.taskmanager.data.TaskState

interface StateStorage {
    fun getState(taskId: TaskId): TaskState?

    fun updateState(taskId: TaskId, newState: TaskState, oldState: TaskState?)

    fun deleteState(taskId: TaskId)
}
