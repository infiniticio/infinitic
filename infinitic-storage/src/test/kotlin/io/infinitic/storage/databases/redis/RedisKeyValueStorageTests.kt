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
package io.infinitic.storage.databases.redis

import io.infinitic.storage.DockerOnly
import io.infinitic.storage.config.RedisConfig
import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.core.annotation.EnabledIf
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import org.testcontainers.containers.GenericContainer
import org.testcontainers.utility.DockerImageName

@EnabledIf(DockerOnly::class)
class RedisKeyValueStorageTests :
  StringSpec(
      {
        val redisServer = GenericContainer(DockerImageName.parse("redis:7.2.3"))
            .withExposedPorts(6379).also { it.start() }
        val config = RedisConfig(host = redisServer.host, port = redisServer.firstMappedPort)
        val storage = RedisKeyValueStorage.from(config)

        afterSpec {
          config.close()
          redisServer.stop()
        }

        beforeTest { storage.put("foo", "bar".toByteArray()) }

        afterTest { storage.flush() }

        "get should return null on unknown key" { storage.get("unknown") shouldBe null }

        "get should return value" {
          storage.get("foo").contentEquals("bar".toByteArray()) shouldBe true
        }

        "put on new key should create value" {
          storage.put("foo2", "bar2".toByteArray())

          storage.get("foo2").contentEquals("bar2".toByteArray()) shouldBe true
        }

        "put on existing key should update value" {
          storage.put("foo", "bar2".toByteArray())

          storage.get("foo").contentEquals("bar2".toByteArray()) shouldBe true
        }

        "del on unknown key does nothing" { storage.put("unknown", null) }

        "del should delete value" {
          storage.put("foo", null)

          storage.get("foo") shouldBe null
        }

        "getSet should return null on unknown key" {
          storage.getSet(setOf("foo", "unknown")) shouldBe mapOf(
              "foo" to "bar".toByteArray(),
              "unknown" to null,
          )
        }

        "putSet on new key should create value" {
          storage.putSet(
              mapOf(
                  "foo2" to "bar2".toByteArray(),
                  "foo3" to "bar3".toByteArray(),
              ),
          )

          storage.getSet(setOf("foo2", "foo3")) shouldBe mapOf(
              "foo2" to "bar2".toByteArray(),
              "foo3" to "bar3".toByteArray(),
          )
        }

        "putSet on existing key should update value" {
          storage.putSet(
              mapOf(
                  "foo" to "bar2".toByteArray(),
                  "foo3" to "bar3".toByteArray(),
              ),
          )

          storage.getSet(setOf("foo", "foo3")) shouldBe mapOf(
              "foo" to "bar2".toByteArray(),
              "foo3" to "bar3".toByteArray(),
          )
        }

        "delSet on unknown key does nothing" {
          shouldNotThrowAny { storage.putSet(mapOf("foo" to null, "unknown" to null)) }

          storage.getSet(setOf("foo", "unknown")) shouldBe mapOf(
              "foo" to null,
              "unknown" to null,
          )
        }
      },
  )
