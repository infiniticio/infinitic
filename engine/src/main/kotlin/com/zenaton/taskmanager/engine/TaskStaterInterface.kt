package com.zenaton.taskmanager.engine

import com.zenaton.taskmanager.state.TaskState

interface TaskStaterInterface {
    fun getState(key: String): TaskState?
    fun createState(key: String, state: TaskState)
    fun updateState(key: String, state: TaskState)
    fun deleteState(key: String)
}
