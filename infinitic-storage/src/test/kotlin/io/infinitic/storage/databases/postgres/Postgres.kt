package io.infinitic.storage.databases.postgres

import io.infinitic.storage.config.PostgresConfig
import org.testcontainers.containers.PostgreSQLContainer

object Postgres {
  fun createServer() = PostgreSQLContainer<Nothing>("postgres:16.2").apply {
    startupAttempts = 1
    withUsername("test")
    withPassword("password")
    withDatabaseName("infinitic")
  }.also { it.start() }

  fun getConfig(server: PostgreSQLContainer<Nothing>) = PostgresConfig(
      host = server.host,
      port = server.firstMappedPort,
      username = server.username,
      password = server.password,
      database = server.databaseName,
      schema = "public",
  )
}
