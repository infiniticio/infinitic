package com.zenaton.taskmanager.engine

import com.zenaton.taskmanager.data.TaskId
import com.zenaton.taskmanager.data.TaskState

interface TaskEngineStateStorage {
    fun getState(taskId: TaskId): TaskState?

    fun updateState(taskId: TaskId, newState: TaskState, oldState: TaskState?)

    fun deleteState(taskId: TaskId)
}
