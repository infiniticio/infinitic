/**
 * "Commons Clause" License Condition v1.0
 *
 * The Software is provided to you by the Licensor under the License, as defined
 * below, subject to the following condition.
 *
 * Without limiting other conditions in the License, the grant of rights under the
 * License will not include, and the License does not grant to you, the right to
 * Sell the Software.
 *
 * For purposes of the foregoing, “Sell” means practicing any or all of the rights
 * granted to you under the License to provide to third parties, for a fee or
 * other consideration (including without limitation fees for hosting or
 * consulting/ support services related to the Software), a product or service
 * whose value derives, entirely or substantially, from the functionality of the
 * Software. Any license notice or attribution required by the License must also
 * include this Commons Clause License Condition notice.
 *
 * Software: Infinitic
 *
 * License: MIT License (https://opensource.org/licenses/MIT)
 *
 * Licensor: infinitic.io
 */

package io.infinitic.cache.config

import com.sksamuel.hoplite.ConfigException
import com.sksamuel.hoplite.ConfigLoaderBuilder
import com.sksamuel.hoplite.yaml.YamlPropertySource
import io.infinitic.cache.config.caffeine.Caffeine
import io.infinitic.cache.config.caffeine.CaffeineCachedKeySet
import io.infinitic.cache.config.caffeine.CaffeineCachedKeyValue
import io.infinitic.cache.config.none.NoCachedKeySet
import io.infinitic.cache.config.none.NoCachedKeyValue
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain

class CacheConfigTests : StringSpec({

    "default cache should be caffeine" {
        val config = loadConfigFromYaml<CacheConfigImpl>("nothing:")

        config shouldBe CacheConfigImpl(cache = Cache(caffeine = Caffeine.default()))
    }

    "cache without type should be caffeine" {
        val config = loadConfigFromYaml<CacheConfigImpl>("cache:")

        config shouldBe CacheConfigImpl(cache = Cache(caffeine = Caffeine.default()))
        config.cache.type shouldBe "caffeine"
        config.cache.keyValue::class shouldBe CaffeineCachedKeyValue::class
        config.cache.keySet::class shouldBe CaffeineCachedKeySet::class
    }

    "can choose none cache" {
        val config = loadConfigFromYaml<CacheConfigImpl>(
            """
cache:
  none:
     """
        )

        config shouldBe CacheConfigImpl(cache = Cache(none = None()))
        config.cache.type shouldBe "none"
        config.cache.keyValue::class shouldBe NoCachedKeyValue::class
        config.cache.keySet::class shouldBe NoCachedKeySet::class
    }

    "can choose Caffeine cache" {
        val config = loadConfigFromYaml<CacheConfigImpl>(
            """
cache:
  caffeine:
     """
        )

        config shouldBe CacheConfigImpl(cache = Cache(caffeine = Caffeine()))
        config.cache.type shouldBe "caffeine"
        config.cache.keyValue::class shouldBe CaffeineCachedKeyValue::class
        config.cache.keySet::class shouldBe CaffeineCachedKeySet::class
    }

    "can choose Caffeine cache with correct values" {
        val config = loadConfigFromYaml<CacheConfigImpl>(
            """
cache:
  caffeine:
    maximumSize: 100
    expireAfterAccess: 42
    expireAfterWrite: 64
     """
        )

        config shouldBe CacheConfigImpl(cache = Cache(caffeine = Caffeine(100, 42, 64)))
        config.cache.type shouldBe "caffeine"
        config.cache.keyValue::class shouldBe CaffeineCachedKeyValue::class
        config.cache.keySet::class shouldBe CaffeineCachedKeySet::class
    }

    "can not have multiple definition in cache" {
        val e = shouldThrow<ConfigException> {
            loadConfigFromYaml<CacheConfigImpl>(
                """
cache:
  none:
  caffeine:
     """
            )
        }
        e.message shouldContain ("Multiple definitions for cache")
    }
})

private inline fun <reified T : Any> loadConfigFromYaml(yaml: String): T = ConfigLoaderBuilder
    .default()
    .also { builder -> builder.addSource(YamlPropertySource(yaml)) }
    .build()
    .loadConfigOrThrow()
