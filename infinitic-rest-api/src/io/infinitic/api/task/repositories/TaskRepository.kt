package io.infinitic.api.task.repositories

import io.infinitic.api.task.models.Task

interface TaskRepository {
    fun getById(id: String): Task?
}
