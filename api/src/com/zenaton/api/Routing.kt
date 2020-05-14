package com.zenaton.api

import com.zenaton.api.extensions.io.ktor.application.*
import com.zenaton.api.task.repositories.TaskRepository
import io.ktor.application.*
import io.ktor.http.HttpStatusCode
import io.ktor.response.*
import io.ktor.routing.*
import org.apache.pulsar.client.admin.PulsarAdmin
import org.koin.ktor.ext.inject

fun Routing.root() {
    val taskRepository: TaskRepository by inject()
    val pulsarAdmin: PulsarAdmin by inject()

    get("/task-types/") {
        val state =
            pulsarAdmin.functions().getFunctionState("public", "default", "MonitoringGlobalPulsarFunction", "monitoringGlobal.state")

        call.respond(state)
    }

    get("/tasks/{id}") {
        val task = taskRepository.getById(call.getPath("id"))
        if (task == null) {
            call.respond(HttpStatusCode.NotFound)
        } else {
            call.respond(task)
        }
    }
}
