package io.infinitic.storage.databases.inMemory

import io.infinitic.storage.keyValue.KeyValueStorageRaceTests

internal class InMemoryKeyValueStorageRaceTests : KeyValueStorageRaceTests() {

  override fun createStorage() = InMemoryKeyValueStorage()

  override suspend fun startServer() {}

  override suspend fun stopServer() {}
}
