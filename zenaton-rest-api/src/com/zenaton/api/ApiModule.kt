package com.zenaton.api

import com.zenaton.api.task.repositories.PrestoJdbcTaskRepository
import com.zenaton.api.task.repositories.TaskRepository
import org.apache.pulsar.client.admin.PulsarAdmin
import org.apache.pulsar.client.impl.conf.ClientConfigurationData
import org.koin.dsl.bind
import org.koin.dsl.module
import java.sql.DriverManager
import java.util.*

val ApiModule = module(createdAtStart = true) {
    single {
        val properties = Properties()
        properties["user"] = "user" // Presto requires setting a user name even if there is no authentication involved

        DriverManager.getConnection("""jdbc:presto://localhost:8081/pulsar""", properties)
    }
    single { PrestoJdbcTaskRepository(get()) } bind TaskRepository::class
    single {
        val config = ClientConfigurationData()
        PulsarAdmin("http://localhost:8080", config)
    }
}
