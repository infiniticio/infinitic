package io.infinitic.storage.config

import com.sksamuel.hoplite.ConfigException
import com.sksamuel.hoplite.ConfigLoaderBuilder
import com.sksamuel.hoplite.yaml.YamlPropertySource
import io.infinitic.storage.config.inMemory.InMemoryKeySetStorage
import io.infinitic.storage.config.inMemory.InMemoryKeyValueStorage
import io.infinitic.storage.config.mysql.MySQLKeySetStorage
import io.infinitic.storage.config.mysql.MySQLKeyValueStorage
import io.infinitic.storage.config.redis.RedisKeySetStorage
import io.infinitic.storage.config.redis.RedisKeyValueStorage
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain

class StorageConfigTests : StringSpec({

    "default storage should be inMemory" {
        val config = loadConfigFromYaml<StorageConfigImpl>("nothing:")

        config shouldBe StorageConfigImpl(storage = Storage(inMemory = InMemory))
    }

    "storage without type should be inMemory" {
        val config = loadConfigFromYaml<StorageConfigImpl>("storage:")

        config shouldBe StorageConfigImpl(storage = Storage(inMemory = InMemory))
        config.storage.type shouldBe "inMemory"
        config.storage.keyValue::class shouldBe InMemoryKeyValueStorage::class
        config.storage.keySet::class shouldBe InMemoryKeySetStorage::class
    }

    "can choose Redis storage" {
        val config = loadConfigFromYaml<StorageConfigImpl>(
            """
storage:
  redis:
     """
        )

        config shouldBe StorageConfigImpl(storage = Storage(redis = Redis()))
        config.storage.type shouldBe "redis"
        config.storage.keyValue::class shouldBe RedisKeyValueStorage::class
        config.storage.keySet::class shouldBe RedisKeySetStorage::class
    }

    "can choose MySQL storage" {
        val config = loadConfigFromYaml<StorageConfigImpl>(
            """
storage:
  mysql:
     """
        )

        config shouldBe StorageConfigImpl(storage = Storage(mysql = MySQL()))
        config.storage.type shouldBe "mysql"
        config.storage.keyValue::class shouldBe MySQLKeyValueStorage::class
        config.storage.keySet::class shouldBe MySQLKeySetStorage::class
    }

    "can not have multiple definition in storage" {
        val e = shouldThrow<ConfigException> {
            loadConfigFromYaml<StorageConfigImpl>(
                """
storage:
  redis:
  mysql:
     """
            )
        }
        e.message shouldContain ("Multiple definitions for storage")
    }
})

private inline fun <reified T : Any> loadConfigFromYaml(yaml: String): T = ConfigLoaderBuilder
    .default()
    .also { builder -> builder.addSource(YamlPropertySource(yaml)) }
    .build()
    .loadConfigOrThrow()
