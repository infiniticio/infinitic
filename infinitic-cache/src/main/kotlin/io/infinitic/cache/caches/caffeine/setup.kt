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

import com.github.benmanes.caffeine.cache.RemovalCause
import io.github.oshai.kotlinlogging.KotlinLogging
import io.infinitic.cache.CacheConfig
import io.infinitic.cache.CaffeineConfig
import java.util.concurrent.TimeUnit
import com.github.benmanes.caffeine.cache.Caffeine as CaffeineCache

internal val logger = KotlinLogging.logger(CacheConfig::class.java.name)

internal fun <S, T> CaffeineCache<S, T>.setup(config: CaffeineConfig): CaffeineCache<S, T> {

  config.maximumSize?.let { maximumSize(it) }
  config.expireAfterAccess?.let { expireAfterAccess(it, TimeUnit.SECONDS) }
  config.expireAfterWrite?.let { expireAfterWrite(it, TimeUnit.SECONDS) }

  removalListener<S, T> { key, _, cause ->
    when (cause) {
      RemovalCause.SIZE -> logger.debug { "Cache size exceeded, removing $key" }
      RemovalCause.EXPIRED -> logger.debug { "Cache expired, removing $key" }
      else -> Unit
    }
  }

  return this
}
