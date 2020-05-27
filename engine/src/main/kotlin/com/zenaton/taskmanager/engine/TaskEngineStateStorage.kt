package com.zenaton.taskmanager.engine

import com.zenaton.taskmanager.data.TaskId
import com.zenaton.taskmanager.data.TaskState

interface TaskEngineStateStorage {
    fun getState(taskId: TaskId): TaskState?
    fun createState(taskId: TaskId, state: TaskState)
    fun updateState(taskId: TaskId, state: TaskState)
    fun deleteState(taskId: TaskId)

//    fun updateState(taskId: TaskId, oldState: TaskState?, newState: TaskState?)
}
