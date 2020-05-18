package com.zenaton.taskmanager.state

interface TaskStaterInterface {
    fun getState(key: String): TaskState?
    fun createState(key: String, state: TaskState)
    fun updateState(key: String, state: TaskState)
    fun deleteState(key: String)
}
