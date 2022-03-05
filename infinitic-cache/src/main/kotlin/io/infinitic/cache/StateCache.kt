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

package io.infinitic.cache

import io.infinitic.cache.caffeine.Caffeine
import io.infinitic.cache.caffeine.CaffeineKeyCounterCache
import io.infinitic.cache.caffeine.CaffeineKeyMapCache
import io.infinitic.cache.caffeine.CaffeineKeySetCache
import io.infinitic.cache.caffeine.CaffeineKeyValueCache
import io.infinitic.cache.none.NoKeyCounterCache
import io.infinitic.cache.none.NoKeyMapCache
import io.infinitic.cache.none.NoKeySetCache
import io.infinitic.cache.none.NoKeyValueCache
import io.infinitic.common.storage.keyCounter.KeyCounterCache
import io.infinitic.common.storage.keyMap.KeyMapCache
import io.infinitic.common.storage.keySet.KeySetCache
import io.infinitic.common.storage.keyValue.KeyValueCache

@Suppress("EnumEntryName")
enum class StateCache : KeyCounter, KeySet, KeyValue, KeyMap {
    none {
        override fun keyCounter(config: CacheConfig) = NoKeyCounterCache<Long>()
        override fun keySet(config: CacheConfig) = NoKeySetCache<ByteArray>()
        override fun keyValue(config: CacheConfig) = NoKeyValueCache<ByteArray>()
        override fun keyMap(config: CacheConfig) = NoKeyMapCache<ByteArray>()
    },
    caffeine {
        override fun keyCounter(config: CacheConfig) = CaffeineKeyCounterCache(caffeineConfig(config))
        override fun keySet(config: CacheConfig) = CaffeineKeySetCache(caffeineConfig(config))
        override fun keyValue(config: CacheConfig) = CaffeineKeyValueCache<ByteArray>(caffeineConfig(config))
        override fun keyMap(config: CacheConfig) = CaffeineKeyMapCache(caffeineConfig(config))

        /**
         * Default Caffeine configuration
         * - expireAfterAccess = 3600
         * - maximumSize = 10000
         */
        private fun caffeineConfig(config: CacheConfig) =
            config.caffeine ?: Caffeine(expireAfterAccess = 3600, maximumSize = 10000)
    }
}

private interface KeyCounter {
    fun keyCounter(config: CacheConfig): KeyCounterCache
}

private interface KeySet {
    fun keySet(config: CacheConfig): KeySetCache<ByteArray>
}

private interface KeyValue {
    fun keyValue(config: CacheConfig): KeyValueCache<ByteArray>
}

private interface KeyMap {
    fun keyMap(config: CacheConfig): KeyMapCache<ByteArray>
}
