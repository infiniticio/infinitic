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

package io.infinitic.common.storage.keyValue

import org.slf4j.Logger
import org.slf4j.LoggerFactory

open class LoggedKeyValueStorage(
    val storage: KeyValueStorage
) : KeyValueStorage {

    val logger: Logger
        get() = LoggerFactory.getLogger(javaClass)

    override suspend fun getValue(key: String): ByteArray? {
        val value = storage.getValue(key)
        logger.debug("key {} - getValue {}", key, value)

        return value
    }

    override suspend fun putValue(key: String, value: ByteArray) {
        logger.debug("key {} - putValue {}", key, value)
        storage.putValue(key, value)
    }

    override suspend fun delValue(key: String) {
        logger.debug("key {} - delValue", key)
        storage.delValue(key)
    }
}
