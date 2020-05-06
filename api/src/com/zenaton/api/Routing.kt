package com.zenaton.api

import com.zenaton.api.extensions.io.ktor.application.*
import com.zenaton.api.task.models.Task
import com.zenaton.api.workflow.models.Workflow
import com.zenaton.api.workflow.repositories.WorkflowRepository
import io.ktor.application.*
import io.ktor.response.*
import io.ktor.routing.*
import org.koin.ktor.ext.inject
import java.time.Instant

fun Routing.root() {
    get("/workflows/{id}") {
        val workflowRepository: WorkflowRepository by this@root.inject()
        val workflow = workflowRepository.getById(call.getPath("id"))

        call.respond(workflow)
    }

    get("/tasks/{id}") {
        val task =
            Task(call.getPath("id"), "TaskA", "completed", Instant.now())
        task.startedAt = Instant.now()
        task.completedAt = Instant.now()

        call.respond(task)
    }
}
