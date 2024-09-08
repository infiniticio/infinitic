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

import io.infinitic.cache.config.CaffeineCacheConfig.CaffeineConfigBuilder
import io.infinitic.cache.keySet.CachedKeySet
import io.infinitic.cache.keyValue.CachedKeyValue
import io.infinitic.config.loadFromYamlFile
import io.infinitic.config.loadFromYamlResource
import io.infinitic.config.loadFromYamlString

sealed interface CacheConfig {
  val keySet: CachedKeySet<ByteArray>?
  val keyValue: CachedKeyValue<ByteArray>?
  val type: String

  companion object {
    @JvmStatic
    fun builder() = CaffeineConfigBuilder()

    /** Create CacheConfig from files in file system */
    @JvmStatic
    fun fromYamlFile(vararg files: String): CacheConfig =
        loadFromYamlFile(*files)

    /** Create CacheConfig from files in resources directory */
    @JvmStatic
    fun fromYamlResource(vararg resources: String): CacheConfig =
        loadFromYamlResource(*resources)

    /** Create CacheConfig from yaml strings */
    @JvmStatic
    fun fromYamlString(vararg yamls: String): CacheConfig =
        loadFromYamlString(*yamls)
  }
}

