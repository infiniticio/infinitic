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

package io.infinitic.storage.inMemory.keySet

import io.infinitic.common.storage.Flushable
import io.infinitic.common.storage.keySet.KeySetStorage
import java.util.concurrent.ConcurrentHashMap

class InMemoryKeySetStorage() : KeySetStorage, Flushable {
    private val storage = ConcurrentHashMap<String, MutableSet<ByteArray>>()

    override suspend fun getSet(key: String): MutableSet<ByteArray> {
        return storage[key]
            ?: run {
                // create empty set
                val set = mutableSetOf<ByteArray>()
                storage[key] = set
                set
            }
    }

    override suspend fun addToSet(key: String, value: ByteArray) {
        getSet(key).add(value)
    }

    override suspend fun removeFromSet(key: String, value: ByteArray) {
        getSet(key).remove(value)
        // clean key if now empty
        if (getSet(key).isEmpty()) storage.remove(key)
    }

    override fun flush() {
        storage.clear()
    }
}