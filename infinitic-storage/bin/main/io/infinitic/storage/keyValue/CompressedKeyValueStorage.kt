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

import io.infinitic.storage.compression.CompressionConfig
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

class CompressedKeyValueStorage(
  private val compressionConfig: CompressionConfig?,
  val storage: KeyValueStorage
) : KeyValueStorage by storage {

  override suspend fun get(key: String): ByteArray? =
  // As the compression method can change over time,
  // we always detect if the state is compressed or not,
      // independently of the provided compressor
      storage.get(key)?.let { decompress(it) }

  override suspend fun put(key: String, bytes: ByteArray?) =
      // apply the provided compression method, if any
      storage.put(key, bytes?.let { compress(it) })

  override suspend fun get(keys: Set<String>): Map<String, ByteArray?> = coroutineScope {
    storage.get(keys)
        .mapValues { async { it.value?.let { v -> decompress(v) } } }
        .mapValues { it.value.await() }
  }

  override suspend fun put(bytes: Map<String, ByteArray?>) = coroutineScope {
    bytes
        .mapValues { async { it.value?.let { v -> compress(v) } } }
        .mapValues { it.value.await() }
        .let { storage.put(it) }
  }

  private fun compress(value: ByteArray) = compressionConfig?.compress(value) ?: value

  private fun decompress(value: ByteArray) = CompressionConfig.decompress(value)
}
