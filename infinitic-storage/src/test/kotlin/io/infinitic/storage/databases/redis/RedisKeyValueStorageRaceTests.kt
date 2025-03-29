package io.infinitic.storage.databases.redis

import io.infinitic.storage.DockerOnly
import io.infinitic.storage.keyValue.KeyValueStorageRaceTests
import io.kotest.core.annotation.EnabledIf

@EnabledIf(DockerOnly::class)
internal class RedisKeyValueStorageRaceTests : KeyValueStorageRaceTests() {
  private val server by lazy { Redis.createServer() }
  private val config by lazy { Redis.getConfig(server) }

  override fun createStorage() = RedisKeyValueStorage.from(config)

  override suspend fun startServer() {
    server.start()
  }

  override suspend fun stopServer() {
    server.stop()
  }
}
