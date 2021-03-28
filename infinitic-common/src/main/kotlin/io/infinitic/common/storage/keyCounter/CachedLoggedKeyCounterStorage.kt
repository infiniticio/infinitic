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

package io.infinitic.common.storage.keyCounter

import org.slf4j.Logger
import org.slf4j.LoggerFactory

class CachedLoggedKeyCounterStorage(
    cache: KeyCounterCache,
    storage: KeyCounterStorage
) : KeyCounterStorage {

    val cache = LoggedKeyCounterCache(cache)
    val storage = LoggedKeyCounterStorage(storage)

    val logger: Logger
        get() = LoggerFactory.getLogger(javaClass)

    override suspend fun getCounter(key: String) = cache.getCounter(key)
        ?: run {
            logger.debug("key {} - getCounter - absent from cache, get from storage", key)
            storage.getCounter(key)
                .also { cache.setCounter(key, it) }
        }

    override suspend fun incrCounter(key: String, amount: Long) {
        cache.incrCounter(key, amount)
        storage.incrCounter(key, amount)
    }

    override fun flush() {
        storage.flush()
        cache.flush()
    }
}
