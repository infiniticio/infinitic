package com.zenaton.api.task.repositories

import com.zenaton.api.task.models.Task

interface TaskRepository {
    fun getById(id: String): Task?
}
