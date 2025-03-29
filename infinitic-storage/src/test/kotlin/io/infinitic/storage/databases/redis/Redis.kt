package io.infinitic.storage.databases.redis

import io.infinitic.storage.config.RedisConfig
import org.testcontainers.containers.GenericContainer

object Redis {
  fun createServer() = GenericContainer<Nothing>("redis:7.2").apply {
    startupAttempts = 1
    withExposedPorts(6379)
    withCommand("redis-server", "--requirepass", "password")
  }.also { it.start() }

  fun getConfig(server: GenericContainer<Nothing>) = RedisConfig(
      host = server.host,
      port = server.firstMappedPort,
      password = "password",
  )
}
