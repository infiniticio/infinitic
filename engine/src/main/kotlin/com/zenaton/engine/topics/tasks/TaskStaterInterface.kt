package com.zenaton.engine.topics.tasks

import com.zenaton.engine.data.tasks.TaskState

interface TaskStaterInterface {
    fun getState(key: String): TaskState?
    fun createState(state: TaskState)
    fun updateState(state: TaskState)
    fun deleteState(state: TaskState)
}
