package com.zenaton.taskManager.engine

import com.zenaton.taskManager.data.TaskId

interface EngineStorage {
    fun getState(taskId: TaskId): EngineState?
    fun updateState(taskId: TaskId, newState: EngineState, oldState: EngineState?)
    fun deleteState(taskId: TaskId)
}
