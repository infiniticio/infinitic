package com.zenaton.taskmanager.metrics

import com.zenaton.taskmanager.messages.events.TaskStatusUpdated
import com.zenaton.taskmanager.state.StateStorage

class TaskMetrics(private val stateStorage: StateStorage) {
    fun handle(msg: TaskStatusUpdated) {
        msg.oldStatus?.let {
            val counterKey = "metrics.rt.counter.task.${msg.taskName.name.toLowerCase()}.${it.toString().toLowerCase()}"
            stateStorage.incrCounter(counterKey, -1)
        }
        msg.newStatus?.let {
            val counterKey = "metrics.rt.counter.task.${msg.taskName.name.toLowerCase()}.${it.toString().toLowerCase()}"
            stateStorage.incrCounter(counterKey, 1)
        }
    }
}
