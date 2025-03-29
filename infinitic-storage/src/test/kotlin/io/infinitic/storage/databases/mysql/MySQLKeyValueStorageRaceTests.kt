package io.infinitic.storage.databases.mysql

import io.infinitic.storage.DockerOnly
import io.infinitic.storage.keyValue.KeyValueStorageRaceTests
import io.kotest.core.annotation.EnabledIf

@EnabledIf(DockerOnly::class)
internal class MySQLKeyValueStorageRaceTests : KeyValueStorageRaceTests() {
  private val server by lazy { MySQL.createServer() }
  private val config by lazy { MySQL.getConfig(server) }

  override fun createStorage() = MySQLKeyValueStorage.from(config)

  override suspend fun startServer() {
    server.start()
  }

  override suspend fun stopServer() {
    server.stop()
  }
}
