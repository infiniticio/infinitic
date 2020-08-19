package io.infinitic.api

import io.infinitic.api.extensions.io.ktor.application.*
import io.infinitic.api.support.BuildInfo
import io.infinitic.api.task.repositories.TaskRepository
import io.infinitic.common.avro.AvroSerDe
import io.infinitic.taskManager.states.AvroMonitoringGlobalState
import io.infinitic.taskManager.states.AvroMonitoringPerNameState
import io.ktor.application.*
import io.ktor.config.ApplicationConfig
import io.ktor.features.NotFoundException
import io.ktor.http.HttpStatusCode
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.util.KtorExperimentalAPI
import org.apache.pulsar.client.admin.PulsarAdmin
import org.apache.pulsar.client.admin.PulsarAdminException
import org.koin.ktor.ext.inject
import java.nio.ByteBuffer

@OptIn(KtorExperimentalAPI::class)
fun Routing.root() {
    val taskRepository: TaskRepository by inject()
    val pulsarAdmin: PulsarAdmin by inject()
    val config: ApplicationConfig by inject()
    val buildInfo: BuildInfo by inject()

    get("/info") {
        call.respond(object {
            val version = buildInfo.version
        })
    }

    get("/task-types/") {
        val state =
            pulsarAdmin.functions().getFunctionState(config.property("infinitic.pulsar.tenant").getString(), config.property("infinitic.pulsar.namespace").getString(), "infinitic-tasks-monitoring-global", "monitoringGlobal.state")?.let { AvroSerDe.deserialize<AvroMonitoringGlobalState>(ByteBuffer.wrap(it.stringValue.toByteArray())) } ?: return@get

        val tasks = state.taskNames.map { object { val name = it } }

        call.respond(tasks)
    }

    get("/task-types/{name}/metrics") {
        val name = call.getPath<String>("name")
        try {
            val state = pulsarAdmin.functions().getFunctionState(config.property("infinitic.pulsar.tenant").getString(), config.property("infinitic.pulsar.namespace").getString(), "infinitic-tasks-monitoring-per-name", "monitoringPerName.state.$name").let { AvroSerDe.deserialize<AvroMonitoringPerNameState>(ByteBuffer.wrap(it.stringValue.toByteArray())) }

            call.respond(object {
                val name = name
                val runningOkCount = state.runningOkCount
                val runningWarningCount = state.runningWarningCount
                val runningErrorCount = state.runningErrorCount
                val terminatedCompletedCount = state.terminatedCompletedCount
                val terminatedCanceledCount = state.terminatedCanceledCount
            })
        } catch (exception: PulsarAdminException.NotFoundException) {
            throw NotFoundException()
        }
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
