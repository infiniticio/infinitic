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
package io.infinitic.cache

import io.infinitic.cache.caches.caffeine.CaffeineCachedKeySet
import io.infinitic.cache.caches.caffeine.CaffeineCachedKeyValue
import io.infinitic.cache.caches.keySet.CachedKeySet
import io.infinitic.cache.caches.keyValue.CachedKeyValue

data class Cache(
  internal val caffeine: Caffeine? = null
) {

  companion object {
    @JvmStatic
    fun from(caffeine: Caffeine) = Cache(caffeine = caffeine)
  }

  val type: CacheType by lazy {
    when {
      caffeine != null -> CacheType.CAFFEINE
      else -> CacheType.NONE
    }
  }

  val keySet: CachedKeySet<ByteArray>? by lazy {
    when {
      caffeine != null -> CaffeineCachedKeySet(caffeine)
      else -> null
    }
  }

  val keyValue: CachedKeyValue<ByteArray>? by lazy {
    when {
      caffeine != null -> CaffeineCachedKeyValue(caffeine)
      else -> null
    }
  }

  enum class CacheType {
    NONE,
    CAFFEINE
  }
}
