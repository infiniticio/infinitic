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
package io.infinitic.cache.caches.caffeine

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import io.infinitic.cache.Flushable
import io.infinitic.cache.config.CaffeineCacheConfig
import io.infinitic.cache.data.Bytes
import io.infinitic.cache.keySet.CachedKeySet

internal class CaffeineCachedKeySet(config: CaffeineCacheConfig) : CachedKeySet<ByteArray>,
  Flushable {

  private val caffeine: Cache<String, Set<Bytes>> = Caffeine.newBuilder().setup(config).build()

  override fun get(key: String): Set<ByteArray>? =
      caffeine.get(key) { null }?.map { it.content }?.toSet()

  override fun set(key: String, value: Set<ByteArray>) {
    caffeine.put(key, value.map { Bytes(it) }.toMutableSet())
  }

  override fun add(key: String, value: ByteArray) {
    caffeine.getIfPresent(key)?.also { caffeine.put(key, it.plus(Bytes(value))) }
  }

  override fun remove(key: String, value: ByteArray) {
    caffeine.getIfPresent(key)?.also { caffeine.put(key, it.minus(Bytes(value))) }
  }

  override fun flush() {
    caffeine.invalidateAll()
  }
}
