// "Commons Clause" License Condition v1.0
//
// The Software is provided to you by the Licensor under the License, as defined
// below, subject to the following condition.
//
// Without limiting other conditions in the License, the grant of rights under the
// License will not include, and the License does not grant to you, the right to
// Sell the Software.
//
// For purposes of the foregoing, “Sell” means practicing any or all of the rights
// granted to you under the License to provide to third parties, for a fee or
// other consideration (including without limitation fees for hosting or
// consulting/ support services related to the Software), a product or service
// whose value derives, entirely or substantially, from the functionality of the
// Software. Any license notice or attribution required by the License must also
// include this Commons Clause License Condition notice.
//
// Software: Infinitic
//
// License: MIT License (https://opensource.org/licenses/MIT)
//
// Licensor: infinitic.io

package io.infinitic.api

import io.infinitic.api.extensions.io.ktor.application.getPath
import io.infinitic.api.support.BuildInfo
import io.infinitic.api.task.repositories.TaskRepository
import io.infinitic.common.monitoringGlobal.state.MonitoringGlobalState
import io.infinitic.common.monitoringPerName.state.MonitoringPerNameState
import io.ktor.application.call
import io.ktor.config.ApplicationConfig
import io.ktor.features.NotFoundException
import io.ktor.http.HttpStatusCode
import io.ktor.response.respond
import io.ktor.routing.Routing
import io.ktor.routing.get
import io.ktor.util.KtorExperimentalAPI
import org.apache.pulsar.client.admin.PulsarAdmin
import org.apache.pulsar.client.admin.PulsarAdminException
import org.koin.ktor.ext.inject

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
            pulsarAdmin.functions().getFunctionState(
                config.property("infinitic.pulsar.tenant").getString(),
                config.property("infinitic.pulsar.namespace").getString(),
                "infinitic-tasks-monitoring-global",
                "monitoringGlobal.state"
            )?.let {
                MonitoringGlobalState.fromByteArray(it.stringValue.toByteArray())
            } ?: return@get

        val tasks = state.taskNames.map { object { val name = it } }

        call.respond(tasks)
    }

    get("/task-types/{name}/metrics") {
        val name = call.getPath<String>("name")
        try {
            val state =
                pulsarAdmin.functions().getFunctionState(
                    config.property("infinitic.pulsar.tenant").getString(),
                    config.property("infinitic.pulsar.namespace").getString(),
                    "infinitic-tasks-monitoring-per-name",
                    "monitoringPerName.state.$name"
                ).let {
                    MonitoringPerNameState.fromByteArray(it.stringValue.toByteArray())
                }

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
