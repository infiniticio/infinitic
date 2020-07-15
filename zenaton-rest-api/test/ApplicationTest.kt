package com.zenaton

import io.ktor.application.*
import io.ktor.response.*
import io.ktor.request.*
import io.ktor.routing.*
import io.ktor.http.*
import io.ktor.auth.*
import com.fasterxml.jackson.databind.*
import com.zenaton.api.module
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
            }
            module(testing = true)
        }) {
            handleRequest(HttpMethod.Get, "/").apply {
                assertFalse(requestHandled)
            }
        }
    }
}
