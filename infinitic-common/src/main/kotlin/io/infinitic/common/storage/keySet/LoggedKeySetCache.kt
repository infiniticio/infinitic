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

import mu.KotlinLogging

class LoggedKeySetCache<T>(
    val cache: KeySetCache<T>
) : KeySetCache<T> by cache {

    private val logger = KotlinLogging.logger {}

    override fun getSet(key: String): Set<T>? {
        val value = cache.getSet(key)
        logger.debug { "key $key - getSet.size ${value?.size}" }

        return value
    }

    override fun setSet(key: String, value: Set<T>) {
        logger.debug { "key $key - setSet.size ${value.size}" }
        cache.setSet(key, value)
    }

    override fun addToSet(key: String, value: T) {
        logger.debug { "key $key - addToSet $value" }
        cache.addToSet(key, value)
    }
    override fun removeFromSet(key: String, value: T) {
        logger.debug { "key $key - removeFromSet $value" }
        cache.removeFromSet(key, value)
    }
}
