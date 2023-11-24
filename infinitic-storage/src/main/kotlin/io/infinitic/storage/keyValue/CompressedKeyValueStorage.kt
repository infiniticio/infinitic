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
package io.infinitic.storage.keyValue

import io.infinitic.storage.compressor.Compressor
import org.jetbrains.annotations.TestOnly

class CompressedKeyValueStorage(private val compressor: Compressor?, val storage: KeyValueStorage) :
  KeyValueStorage {

  override suspend fun get(key: String): ByteArray? =
  // As the compression method can change over time,
  // we always detect if the state is compressed or not
      // independently of the provided compressor
      storage.get(key)?.let { Compressor.decompress(it) }

  override suspend fun put(key: String, value: ByteArray) = try {
    // apply the provided compression method, if any
    storage.put(key, compressor?.compress(value) ?: value)
  } catch (e: Exception) {
    throw KeyValueStorageException(e)
  }

  override suspend fun del(key: String) = try {
    storage.del(key)
  } catch (e: Exception) {
    throw KeyValueStorageException(e)
  }

  @TestOnly
  override fun flush() = try {
    storage.flush()
  } catch (e: Exception) {
    throw KeyValueStorageException(e)
  }
}
