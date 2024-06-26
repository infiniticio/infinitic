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
package io.infinitic.cache.config

import io.infinitic.cache.config.caffeine.Caffeine
import io.infinitic.cache.config.caffeine.CaffeineCachedKeySet
import io.infinitic.cache.config.caffeine.CaffeineCachedKeyValue
import io.infinitic.cache.config.none.NoCachedKeySet
import io.infinitic.cache.config.none.NoCachedKeyValue
import io.infinitic.cache.keySet.CachedKeySet
import io.infinitic.cache.keyValue.CachedKeyValue

data class Cache(
  var none: None? = null,
  val caffeine: Caffeine? = null
) {

  init {
    val nonNul = listOfNotNull(none, caffeine)

    if (nonNul.isEmpty()) {
      // No cache  by default
      none = None()
    } else {
      require(nonNul.count() == 1) { "Cache should have only one definition: ${nonNul.joinToString { it::class.java.simpleName }}" }
    }
  }

  val type: String by lazy {
    when {
      none != null -> "none"
      caffeine != null -> "caffeine"
      else -> throw RuntimeException("This should not happen")
    }
  }

  val keySet: CachedKeySet<ByteArray> by lazy {
    when {
      none != null -> NoCachedKeySet()
      caffeine != null -> CaffeineCachedKeySet(caffeine)
      else -> throw RuntimeException("This should not happen")
    }
  }

  val keyValue: CachedKeyValue<ByteArray> by lazy {
    when {
      none != null -> NoCachedKeyValue()
      caffeine != null -> CaffeineCachedKeyValue(caffeine)
      else -> throw RuntimeException("This should not happen")
    }
  }
}
