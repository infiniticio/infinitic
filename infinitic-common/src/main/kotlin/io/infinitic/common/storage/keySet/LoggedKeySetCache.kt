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

package io.infinitic.common.storage.keySet

import org.slf4j.Logger
import org.slf4j.LoggerFactory

class LoggedKeySetCache<T>(
    val cache: KeySetCache<T>
) : KeySetCache<T> by cache {

    val logger: Logger
        get() = LoggerFactory.getLogger(javaClass)

    override fun getSet(key: String): Set<T>? {
        val value = cache.getSet(key)
        logger.debug("key {} - getSet.size {}", key, value?.size)

        return value
    }

    override fun setSet(key: String, value: Set<T>) {
        logger.debug("key {} - setSet.size {}", key, value.size)
        cache.setSet(key, value)
    }

    override fun addToSet(key: String, value: T) {
        logger.debug("key {} - addToSet {}", key, value)
        cache.addToSet(key, value)
    }
    override fun removeFromSet(key: String, value: T) {
        logger.debug("key {} - removeFromSet {}", key, value)
        cache.removeFromSet(key, value)
    }
}
