package com.zenaton

import io.ktor.application.*
import io.ktor.response.*
import io.ktor.request.*
import io.ktor.routing.*
import io.ktor.http.*
import io.ktor.auth.*
import com.fasterxml.jackson.databind.*
import com.zenaton.extensions.*
import io.ktor.jackson.*
import io.ktor.features.*
import java.time.Instant

fun main(args: Array<String>): Unit = io.ktor.server.netty.EngineMain.main(args)

@Suppress("unused") // Referenced in application.conf
@kotlin.jvm.JvmOverloads
fun Application.module(testing: Boolean = false) {
    install(Authentication) {
    }

    install(ContentNegotiation) {
        jackson {
            enable(SerializationFeature.INDENT_OUTPUT)
        }
    }

    install(CORS) {
        method(HttpMethod.Options)
        method(HttpMethod.Put)
        method(HttpMethod.Delete)
        method(HttpMethod.Patch)
        header(HttpHeaders.Authorization)
        header("MyCustomHeader")
        allowCredentials = true
        anyHost() // @TODO: Don't do this in production if possible. Try to limit it.
    }

    routing {
        get("/workflows/{id}") {
            val workflow = Workflow(call.getPath("id"), "SequentialWorkflow", "running", listOf("user:123"), Instant.now(), Instant.now())

            call.respond(workflow)
        }

        get("/tasks/{id}") {
            val task = Task(call.getPath("id"), "TaskA", "completed", Instant.now())
            task.startedAt = Instant.now()
            task.completedAt = Instant.now()

            call.respond(task)
        }
    }
}
