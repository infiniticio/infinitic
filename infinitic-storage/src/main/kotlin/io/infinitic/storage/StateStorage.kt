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

import io.infinitic.common.storage.keyCounter.KeyCounterStorage
import io.infinitic.common.storage.keySet.KeySetStorage
import io.infinitic.common.storage.keyValue.KeyValueStorage
import io.infinitic.storage.inMemory.InMemoryKeyCounterStorage
import io.infinitic.storage.inMemory.InMemoryKeySetStorage
import io.infinitic.storage.inMemory.InMemoryKeyValueStorage
import io.infinitic.storage.redis.Redis
import io.infinitic.storage.redis.RedisKeyCounterStorage
import io.infinitic.storage.redis.RedisKeySetStorage
import io.infinitic.storage.redis.RedisKeyValueStorage

@Suppress("EnumEntryName")
enum class StateStorage : KeyCounter, KeySet, KeyValue {
    inMemory {
        override fun keyCounter(config: StorageConfig) = InMemoryKeyCounterStorage()
        override fun keySet(config: StorageConfig) = InMemoryKeySetStorage()
        override fun keyValue(config: StorageConfig) = InMemoryKeyValueStorage()
    },
    redis {
        override fun keyCounter(config: StorageConfig) = RedisKeyCounterStorage.of(redisConfig(config))
        override fun keySet(config: StorageConfig) = RedisKeySetStorage.of(redisConfig(config))
        override fun keyValue(config: StorageConfig) = RedisKeyValueStorage.of(redisConfig(config))

        private fun redisConfig(config: StorageConfig) = config.redis ?: Redis()
    }
}

private interface KeyCounter {
    fun keyCounter(config: StorageConfig): KeyCounterStorage
}

private interface KeySet {
    fun keySet(config: StorageConfig): KeySetStorage
}

private interface KeyValue {
    fun keyValue(config: StorageConfig): KeyValueStorage
}
