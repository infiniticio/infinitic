/**
 * "Commons Clause" License Condition v1.0
 *
 * The Software is provided to you by the Licensor under the License, as defined
 * below, subject to the following condition.
 *
 * Without limiting other conditions in the License, the grant of rights under the
 * License will not include, and the License does not grant to you, the right to
 * Sell the Software.
 *
 * For purposes of the foregoing, “Sell” means practicing any or all of the rights
 * granted to you under the License to provide to third parties, for a fee or
 * other consideration (including without limitation fees for hosting or
 * consulting/ support services related to the Software), a product or service
 * whose value derives, entirely or substantially, from the functionality of the
 * Software. Any license notice or attribution required by the License must also
 * include this Commons Clause License Condition notice.
 *
 * Software: Infinitic
 *
 * License: MIT License (https://opensource.org/licenses/MIT)
 *
 * Licensor: infinitic.io
 */

package io.infinitic.storage

import io.infinitic.storage.inMemory.InMemoryKeySetStorage
import io.infinitic.storage.inMemory.InMemoryKeyValueStorage
import io.infinitic.storage.keySet.KeySetStorage
import io.infinitic.storage.keyValue.KeyValueStorage
import io.infinitic.storage.mysql.MySQL
import io.infinitic.storage.mysql.MySQLKeySetStorage
import io.infinitic.storage.mysql.MySQLKeyValueStorage
import io.infinitic.storage.redis.Redis
import io.infinitic.storage.redis.RedisKeySetStorage
import io.infinitic.storage.redis.RedisKeyValueStorage

@Suppress("EnumEntryName", "unused")
enum class StateStorage : KeySet, KeyValue {
    inMemory {
        override fun keySet(config: StorageConfig) = InMemoryKeySetStorage()
        override fun keyValue(config: StorageConfig) = InMemoryKeyValueStorage()
    },
    redis {
        override fun keySet(config: StorageConfig) = RedisKeySetStorage.of(redisConfig(config))
        override fun keyValue(config: StorageConfig) = RedisKeyValueStorage.of(redisConfig(config))

        private fun redisConfig(config: StorageConfig) = config.redis ?: Redis()
    },
    mysql {
        override fun keySet(config: StorageConfig) = MySQLKeySetStorage.of(mysqlConfig(config))
        override fun keyValue(config: StorageConfig) = MySQLKeyValueStorage.of(mysqlConfig(config))

        private fun mysqlConfig(config: StorageConfig) = config.mysql ?: MySQL()
    }
}

private interface KeySet {
    fun keySet(config: StorageConfig): KeySetStorage
}

private interface KeyValue {
    fun keyValue(config: StorageConfig): KeyValueStorage
}
