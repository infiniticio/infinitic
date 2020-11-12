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

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import io.infinitic.api.support.BuildInfo
import io.infinitic.api.task.repositories.PrestoJdbcTaskRepository
import io.infinitic.api.task.repositories.TaskRepository
import io.ktor.application.Application
import io.ktor.application.ApplicationEnvironment
import io.ktor.application.install
import io.ktor.auth.Authentication
import io.ktor.config.ApplicationConfig
import io.ktor.features.CORS
import io.ktor.features.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.jackson.jackson
import io.ktor.routing.routing
import io.ktor.util.KtorExperimentalAPI
import org.apache.pulsar.client.admin.PulsarAdmin
import org.apache.pulsar.client.impl.conf.ClientConfigurationData
import org.koin.core.KoinApplication
import org.koin.dsl.bind
import org.koin.dsl.module
import org.koin.ktor.ext.Koin
import java.io.FileNotFoundException
import java.sql.DriverManager
import java.util.Properties

fun main(args: Array<String>): Unit = io.ktor.server.netty.EngineMain.main(args)

@Suppress("unused") // Referenced in application.conf
@kotlin.jvm.JvmOverloads
fun Application.module(testing: Boolean = false) {
    install(Authentication) {
    }

    install(ContentNegotiation) {
        jackson {
            enable(SerializationFeature.INDENT_OUTPUT)
            disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            registerModule(JavaTimeModule())
        }
    }

    install(CORS) {
        method(HttpMethod.Options)
        method(HttpMethod.Put)
        method(HttpMethod.Delete)
        method(HttpMethod.Patch)
        header(HttpHeaders.Authorization)
        allowCredentials = true
        anyHost() // @TODO: Don't do this in production if possible. Try to limit it.
    }

    install(Koin) {
        installModules(environment)
    }

    routing {
        root()
    }
}

@OptIn(KtorExperimentalAPI::class)
fun KoinApplication.installModules(environment: ApplicationEnvironment) {
    val apiModule = module(createdAtStart = true) {
        single {
            val inputStream = javaClass.classLoader.getResourceAsStream("build-info.properties") ?: throw FileNotFoundException("Unable to find the build-info.properties file.")
            val properties = Properties()
            properties.load(inputStream)

            BuildInfo(properties)
        }

        single { environment.config } bind ApplicationConfig::class

        single {
            val properties = Properties()
            properties["user"] = "user" // Presto requires setting a user name even if there is no authentication involved

            DriverManager.getConnection(environment.config.property("infinitic.pulsar.presto.url").getString(), properties)
        }

        single { PrestoJdbcTaskRepository(get()) } bind TaskRepository::class

        single {
            val config = ClientConfigurationData()
            PulsarAdmin(environment.config.property("infinitic.pulsar.admin.url").getString(), config)
        }
    }

    modules(apiModule)
}
