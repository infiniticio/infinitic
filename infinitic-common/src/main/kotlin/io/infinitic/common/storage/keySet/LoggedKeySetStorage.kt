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

class LoggedKeySetStorage(
    val storage: KeySetStorage
) : KeySetStorage by storage {

    val logger: Logger
        get() = LoggerFactory.getLogger(javaClass)

    override suspend fun getSet(key: String): Set<ByteArray> {
        val value = storage.getSet(key)
        logger.debug("key {} - getSet.size {}", key, value.size)

        return value
    }

    override suspend fun addToSet(key: String, value: ByteArray) {
        logger.debug("key {} - addToSet {}", key, value)
        storage.addToSet(key, value)
    }

    override suspend fun removeFromSet(key: String, value: ByteArray) {
        logger.debug("key {} - removeFromSet {}", key, value)
        storage.removeFromSet(key, value)
    }
}
