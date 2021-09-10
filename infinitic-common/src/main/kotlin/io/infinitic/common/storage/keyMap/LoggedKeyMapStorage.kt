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

package io.infinitic.common.storage.keyMap

import mu.KotlinLogging

class LoggedKeyMapStorage(
    val storage: KeyMapStorage
) : KeyMapStorage by storage {

    private val logger = KotlinLogging.logger {}

    override suspend fun get(key: String, field: String): ByteArray? {
        val value = storage.get(key, field)
        logger.debug { "key $key - get field $field value $value" }

        return value
    }

    override suspend fun get(key: String): Map<String, ByteArray>? {
        val map = storage.get(key)
        logger.debug { "key $key - get fields ${map?.keys?.joinToString(", ")}" }

        return map
    }

    override suspend fun put(key: String, field: String, value: ByteArray) {
        logger.debug { "key $key - put field $field value $value" }
        storage.put(key, field, value)
    }

    override suspend fun put(key: String, map: Map<String, ByteArray>) {
        logger.debug { "key $key - put fields ${map.keys.joinToString(", ")}" }
        storage.put(key, map)
    }

    override suspend fun del(key: String, field: String) {
        logger.debug { "key $key - del field $field" }
        storage.del(key, field)
    }

    override suspend fun del(key: String) {
        logger.debug { "key $key - del" }
        storage.del(key)
    }
}
