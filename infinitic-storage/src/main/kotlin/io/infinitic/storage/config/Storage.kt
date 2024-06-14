/**
 * "Commons Clause" License Condition v1.0
 *
 * The Software is provided to you by the Licensor under the License, as defined below, subject to
 * the following condition.
 *
 * Without limiting other conditions in the License, the grant of rights under the License will not
 * include, and the License does not grant to you, the right to Sell the Software.
 *
 * For purposes of the foregoing, “Sell” means practicing any or all of the rights granted to you
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
package io.infinitic.storage.config

import io.infinitic.storage.compressor.Compressor
import io.infinitic.storage.databases.inMemory.InMemoryKeySetStorage
import io.infinitic.storage.databases.inMemory.InMemoryKeyValueStorage
import io.infinitic.storage.databases.mysql.MySQLKeySetStorage
import io.infinitic.storage.databases.mysql.MySQLKeyValueStorage
import io.infinitic.storage.databases.postgres.PostgresKeySetStorage
import io.infinitic.storage.databases.postgres.PostgresKeyValueStorage
import io.infinitic.storage.databases.redis.RedisKeySetStorage
import io.infinitic.storage.databases.redis.RedisKeyValueStorage
import io.infinitic.storage.keySet.KeySetStorage
import io.infinitic.storage.keyValue.CompressedKeyValueStorage
import io.infinitic.storage.keyValue.KeyValueStorage

data class Storage(
  var inMemory: InMemory? = null,
  val redis: Redis? = null,
  val mysql: MySQL? = null,
  val postgres: Postgres? = null,
  val compression: Compressor? = null
) {
  init {
    val nonNul = listOfNotNull(inMemory, redis, mysql, postgres)

    if (nonNul.isEmpty()) {
      // default storage is inMemory
      inMemory = InMemory()
    } else {
      require(nonNul.count() == 1) { "Storage should not have multiple definitions: ${nonNul.joinToString { it::class.java.simpleName }}" }
    }
  }

  fun close() {
    when {
      inMemory != null -> inMemory!!.close()
      redis != null -> redis.close()
      mysql != null -> mysql.close()
      postgres != null -> postgres.close()
      else -> thisShouldNotHappen()
    }
  }

  val type by lazy {
    when {
      inMemory != null -> "inMemory"
      redis != null -> "redis"
      mysql != null -> "mysql"
      postgres != null -> "postgres"
      else -> thisShouldNotHappen()
    }
  }

  val keySet: KeySetStorage by lazy {
    when {
      inMemory != null -> InMemoryKeySetStorage.from(inMemory!!)
      redis != null -> RedisKeySetStorage.from(redis)
      mysql != null -> MySQLKeySetStorage.from(mysql)
      postgres != null -> PostgresKeySetStorage.from(postgres)
      else -> thisShouldNotHappen()
    }
  }

  val keyValue: KeyValueStorage by lazy {
    when {
      inMemory != null -> InMemoryKeyValueStorage.from(inMemory!!)
      redis != null -> RedisKeyValueStorage.from(redis)
      mysql != null -> MySQLKeyValueStorage.from(mysql)
      postgres != null -> PostgresKeyValueStorage.from(postgres)
      else -> thisShouldNotHappen()
    }.let { CompressedKeyValueStorage(compression, it) }
  }

  private fun thisShouldNotHappen(): Nothing {
    throw RuntimeException("This should not happen")
  }
}
