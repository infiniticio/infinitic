package com.zenaton.taskmanager.engine.state

import com.zenaton.taskmanager.data.TaskId

interface TaskEngineStateStorage {
    fun getState(taskId: TaskId): TaskEngineState?
    fun updateState(taskId: TaskId, newState: TaskEngineState, oldState: TaskEngineState?)
    fun deleteState(taskId: TaskId)
}
