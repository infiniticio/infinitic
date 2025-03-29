package io.infinitic.storage.databases.postgres

import io.infinitic.storage.DockerOnly
import io.infinitic.storage.keyValue.KeyValueStorageRaceTests
import io.kotest.core.annotation.EnabledIf

@EnabledIf(DockerOnly::class)
internal class PostgresKeyValueStorageRaceTests : KeyValueStorageRaceTests() {
  private val server by lazy { Postgres.createServer() }
  private val config by lazy { Postgres.getConfig(server) }

  override fun createStorage() = PostgresKeyValueStorage.from(config)

  override suspend fun startServer() {
    server.start()
  }

  override suspend fun stopServer() {
    server.stop()
  }
}
