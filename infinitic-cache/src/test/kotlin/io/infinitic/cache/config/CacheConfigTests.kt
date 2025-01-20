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
import io.infinitic.cache.caches.caffeine.CaffeineCachedKeySet
import io.infinitic.cache.caches.caffeine.CaffeineCachedKeyValue
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class CacheConfigTests :
  StringSpec(
      {
        "default cache should be None" {
          val config = loadConfigFromYaml<CacheConfigImpl>("nothing:")

          config shouldBe CacheConfigImpl(cache = null)
        }

        "can set null cache " {
          val config = loadConfigFromYaml<CacheConfigImpl>("cache: null")

          config shouldBe CacheConfigImpl(cache = null)
        }

        "default cache should be Caffeine" {
          val config = loadConfigFromYaml<CacheConfigImpl>("cache:")

          config shouldBe CacheConfigImpl(cache = CaffeineCacheConfig())
          config.cache?.keyValue!!::class shouldBe CaffeineCachedKeyValue::class
          config.cache.keySet!!::class shouldBe CaffeineCachedKeySet::class
        }

        "can choose Caffeine cache with correct values" {
          val config = loadConfigFromYaml<CacheConfigImpl>(
              """
cache:
  maximumSize: 100
  expireAfterAccess: 42
  expireAfterWrite: 64
     """,
          )

          config shouldBe CacheConfigImpl(
              cache = CaffeineCacheConfig(
                  100,
                  42,
                  64,
              ),
          )
        }
      },
  )

private inline fun <reified T : Any> loadConfigFromYaml(yaml: String): T =
    ConfigLoaderBuilder.default()
        .also { builder -> builder.addSource(YamlPropertySource(yaml)) }
        .build()
        .loadConfigOrThrow()
