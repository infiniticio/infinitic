/**
 * "Commons Clause" License Condition v1.0
 *
 * The Software is provided to you by the Licensor under the License, as defined below, subject to
 * the following condition.
 *
 * Without limiting other conditions in the License, the grant of rights under the License will not
 * include, and the License does not grant to you, the right to Sell the Software.
 *
 * For purposes of the foregoing, "Sell" means practicing any or all of the rights granted to you
 * under the License to provide to third parties, for a fee or other consideration (including
 * without limitation fees for hosting or consulting/ support services related to the Software), a
 * product or service whose value derives, entirely or substantially, from the functionality of the
 * Software. Any license notice or attribution required by the License must also include this
 * Commons Clause License Condition notice.
 *
 * Software: Infinitic
 *
 * License: MIT License (https://opensource.org/licenses/MIT)
 *
 * Licensor: infinitic.io
 */
package io.infinitic.storage.databases.postgres

import io.infinitic.storage.DockerOnly
import io.infinitic.storage.keyValue.KeyValueStorageTests
import io.kotest.core.annotation.EnabledIf

@EnabledIf(DockerOnly::class)
internal class PostgresKeyValueStorageTests : KeyValueStorageTests() {
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
