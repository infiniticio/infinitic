/**
 * "Commons Clause" License Condition v1.0
 *
 * The Software is provided to you by the Licensor under the License, as defined below, subject to
 * the following condition.
 *
 * Without limiting other conditions in the License, the grant of rights under the License will not
 * include, and the License does not grant to you, the right to Sell the Software.
 *
 * For purposes of the foregoing, “Sell” means practicing any or all of the rights granted to you
 * under the License to provide to third parties, for a fee or other consideration (including
 * without limitation fees for hosting or consulting/ support services related to the Software), a
 * product or service whose value derives, entirely or substantially, from the functionality of the
 * Software. Any license notice or attribution required by the License must also include this
 * Commons Clause License Condition notice.
 *
 * Software: Infinitic
 *
 * License: MIT License (https://opensource.org/licenses/MIT)
 *
 * Licensor: infinitic.io
 */
package io.infinitic.storage.keySet

import org.jetbrains.annotations.TestOnly

class WrappedKeySetStorage(val storage: KeySetStorage) : KeySetStorage {

  override suspend fun get(key: String) =
      try {
        storage.get(key)
      } catch (e: Exception) {
        throwWrappedException(e)
      }

  override suspend fun add(key: String, value: ByteArray) =
      try {
        storage.add(key, value)
      } catch (e: Exception) {
        throwWrappedException(e)
      }

  override suspend fun remove(key: String, value: ByteArray) =
      try {
        storage.remove(key, value)
      } catch (e: Exception) {
        throwWrappedException(e)
      }

  override suspend fun get(keys: Set<String>): Map<String, Set<ByteArray>> =
      try {
        storage.get(keys)
      } catch (e: Exception) {
        throwWrappedException(e)
      }

  override suspend fun update(
    add: Map<String, Set<ByteArray>>,
    remove: Map<String, Set<ByteArray>>
  ) = try {
    storage.update(add, remove)
  } catch (e: Exception) {
    throwWrappedException(e)
  }

  @TestOnly
  override fun flush() =
      try {
        storage.flush()
      } catch (e: Exception) {
        throwWrappedException(e)
      }

  override fun close() {
    try {
      storage.close()
    } catch (e: Exception) {
      throwWrappedException(e)
    }
  }

  private fun throwWrappedException(e: Exception): Nothing = throw KeySetStorageException(e)
}

