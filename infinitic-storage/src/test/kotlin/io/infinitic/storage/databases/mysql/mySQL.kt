package io.infinitic.storage.databases.mysql

import io.infinitic.storage.config.MySQLConfig
import org.testcontainers.containers.MySQLContainer

internal object MySQL {
  fun createServer() = MySQLContainer<Nothing>("mysql:8.3")
      .apply {
        startupAttempts = 1
        withUsername("test")
        withPassword("password")
        withDatabaseName("infinitic")
      }
      .also { it.start() }

  fun getConfig(server: MySQLContainer<Nothing>) = MySQLConfig(
      host = server.host,
      port = server.firstMappedPort,
      username = server.username,
      password = server.password,
  ).copy(database = server.databaseName)
}
