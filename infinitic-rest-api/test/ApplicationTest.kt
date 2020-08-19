package io.infinitic

import io.ktor.application.*
import io.ktor.response.*
import io.ktor.request.*
import io.ktor.routing.*
import io.ktor.http.*
import io.ktor.auth.*
import com.fasterxml.jackson.databind.*
import io.infinitic.api.module
import io.ktor.config.MapApplicationConfig
import io.ktor.jackson.*
import io.ktor.features.*
import kotlin.test.*
import io.ktor.server.testing.*
import io.ktor.util.KtorExperimentalAPI

class ApplicationTest {
    @OptIn(KtorExperimentalAPI::class)
    @Test
    fun testRoot() {
        withTestApplication({
            (environment.config as MapApplicationConfig).apply {
                put("infinitic.pulsar.admin.url", "http://localhost:8080")
                put("infinitic.pulsar.presto.url", "jdbc:presto://localhost:8081/pulsar")
            }
            module(testing = true)
        }) {
            handleRequest(HttpMethod.Get, "/").apply {
                assertFalse(requestHandled)
            }
        }
    }
}
