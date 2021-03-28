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

package io.infinitic.cache.caffeine

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import io.infinitic.common.storage.Flushable
import io.infinitic.common.storage.keyCounter.KeyCounterCache
import io.infinitic.cache.caffeine.Caffeine as CaffeineConfig

class CaffeineKeyCounterCache(config: CaffeineConfig) : KeyCounterCache, Flushable {
    private var caffeine: Cache<String, Long> =
        Caffeine.newBuilder().setup(config).build()

    override fun getCounter(key: String): Long? = caffeine.getIfPresent(key)

    override fun setCounter(key: String, amount: Long) {
        caffeine.put(key, amount)
    }

    override fun incrCounter(key: String, amount: Long) {
        getCounter(key)?.also {
            caffeine.put(key, it + amount)
        }
    }

    override fun flush() {
        caffeine.invalidateAll()
    }
}
