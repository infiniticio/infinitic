package com.zenaton.api

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.zenaton.api.support.BuildInfo
import com.zenaton.api.task.repositories.PrestoJdbcTaskRepository
import com.zenaton.api.task.repositories.TaskRepository
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
import org.koin.core.module.Module
import org.koin.dsl.bind
import org.koin.dsl.module
import org.koin.ktor.ext.Koin
import java.io.FileNotFoundException
import java.lang.Exception
import java.lang.RuntimeException
import java.sql.DriverManager
import java.util.*

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
