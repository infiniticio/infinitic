package com.zenaton.engine.data.tasks

class TaskState(val taskId: TaskId) {
    fun getKey() = taskId.id
}
