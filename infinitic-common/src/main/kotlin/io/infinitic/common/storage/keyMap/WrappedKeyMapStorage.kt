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

import org.jetbrains.annotations.TestOnly

class WrappedKeyMapStorage(
    val storage: KeyMapStorage
) : KeyMapStorage {

    override suspend fun get(key: String, field: String) = try {
        storage.get(key, field)
    } catch (e: Throwable) {
        throw KeyMapStorageException(e)
    }

    override suspend fun get(key: String) = try {
        storage.get(key)
    } catch (e: Throwable) {
        throw KeyMapStorageException(e)
    }

    override suspend fun put(key: String, field: String, value: ByteArray) = try {
        storage.put(key, field, value)
    } catch (e: Throwable) {
        throw KeyMapStorageException(e)
    }

    override suspend fun put(key: String, map: Map<String, ByteArray>) = try {
        storage.put(key, map)
    } catch (e: Throwable) {
        throw KeyMapStorageException(e)
    }

    override suspend fun del(key: String, field: String) = try {
        storage.del(key, field)
    } catch (e: Throwable) {
        throw KeyMapStorageException(e)
    }

    override suspend fun del(key: String) = try {
        storage.del(key)
    } catch (e: Throwable) {
        throw KeyMapStorageException(e)
    }

    @TestOnly
    override fun flush() = try {
        storage.flush()
    } catch (e: Throwable) {
        throw KeyMapStorageException(e)
    }
}
