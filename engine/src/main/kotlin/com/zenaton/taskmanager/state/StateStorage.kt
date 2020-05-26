package com.zenaton.taskmanager.state

import com.zenaton.taskmanager.data.TaskState

interface StateStorage {
    fun getState(key: String): TaskState?
    fun createState(key: String, state: TaskState)
    fun updateState(key: String, state: TaskState)
    fun deleteState(key: String)
    fun incrCounter(key: String, amount: Long)
}
