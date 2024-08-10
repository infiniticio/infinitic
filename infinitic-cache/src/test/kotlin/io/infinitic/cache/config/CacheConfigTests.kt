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

import com.sksamuel.hoplite.ConfigLoaderBuilder
import com.sksamuel.hoplite.yaml.YamlPropertySource
import io.infinitic.cache.CacheConfig
import io.infinitic.cache.Caffeine
import io.infinitic.cache.caches.caffeine.CaffeineCachedKeySet
import io.infinitic.cache.caches.caffeine.CaffeineCachedKeyValue
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class CacheConfigTests :
  StringSpec(
      {
        "default cache should be None" {
          val config = loadConfigFromYaml<CacheConfigImpl>("nothing:")

          config shouldBe CacheConfigImpl(cacheConfig = CacheConfig())
          config.cacheConfig.type shouldBe CacheConfig.CacheType.NONE
          config.cacheConfig.keyValue shouldBe null
          config.cacheConfig.keySet shouldBe null
        }

        "cache without type should be None" {
          val config = loadConfigFromYaml<CacheConfigImpl>("cache:")

          config shouldBe CacheConfigImpl(cacheConfig = CacheConfig())
          config.cacheConfig.type shouldBe CacheConfig.CacheType.NONE
          config.cacheConfig.keyValue shouldBe null
          config.cacheConfig.keySet shouldBe null
        }

        "can choose Caffeine cache" {
          val config = loadConfigFromYaml<CacheConfigImpl>(
              """
cache:
  caffeine:
     """,
          )
          config shouldBe CacheConfigImpl(cacheConfig = CacheConfig(caffeine = Caffeine()))
          config.cacheConfig.type shouldBe CacheConfig.CacheType.CAFFEINE
          config.cacheConfig.keyValue!!::class shouldBe CaffeineCachedKeyValue::class
          config.cacheConfig.keySet!!::class shouldBe CaffeineCachedKeySet::class
        }

        "can choose Caffeine cache with correct values" {
          val config = loadConfigFromYaml<CacheConfigImpl>(
              """
cache:
  caffeine:
    maximumSize: 100
    expireAfterAccess: 42
    expireAfterWrite: 64
     """,
          )

          config shouldBe CacheConfigImpl(
              cacheConfig = CacheConfig(
                  caffeine = Caffeine(
                      100,
                      42,
                      64,
                  ),
              ),
          )
          config.cacheConfig.type shouldBe CacheConfig.CacheType.CAFFEINE
          config.cacheConfig.keyValue!!::class shouldBe CaffeineCachedKeyValue::class
          config.cacheConfig.keySet!!::class shouldBe CaffeineCachedKeySet::class
        }
      },
  )

private inline fun <reified T : Any> loadConfigFromYaml(yaml: String): T =
    ConfigLoaderBuilder.default()
        .also { builder -> builder.addSource(YamlPropertySource(yaml)) }
        .build()
        .loadConfigOrThrow()
