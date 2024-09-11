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
package io.infinitic.workers.config

import io.infinitic.storage.config.InMemoryStorageConfig
import io.infinitic.workers.samples.ServiceA
import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.shouldBeInstanceOf

internal class ServiceTagEngineConfigTests : StringSpec(
    {
      val serviceName = ServiceA::class.java.name
      val storage = InMemoryStorageConfig.builder().build()

      "Can create ServiceTagEngineConfig through builder with default parameters" {
        val config = shouldNotThrowAny {
          ServiceTagEngineConfig.builder()
              .setServiceName(serviceName)
              .build()
        }

        config.serviceName shouldBe serviceName
        config.shouldBeInstanceOf<ServiceTagEngineConfig>()
        config.storage shouldBe null
        config.concurrency shouldBe 1
      }

      "Can create ServiceTagEngineConfig through builder with all parameters" {
        val config = shouldNotThrowAny {
          ServiceTagEngineConfig.builder()
              .setServiceName(serviceName)
              .setConcurrency(10)
              .setStorage(storage)
              .build()
        }

        config.shouldBeInstanceOf<ServiceTagEngineConfig>()
        config.concurrency shouldBe 10
        config.storage shouldBe storage
      }

      "ServiceName is mandatory when building ServiceTagEngineConfig through builder" {
        val e = shouldThrow<IllegalArgumentException> {
          ServiceTagEngineConfig.builder()
              .build()
        }
        e.message shouldContain "serviceName"
      }

      "Concurrency must be positive when building ServiceTagEngineConfig" {
        val e = shouldThrow<IllegalArgumentException> {
          ServiceTagEngineConfig.builder()
              .setServiceName(serviceName)
              .setConcurrency(0)
              .build()
        }
        e.message shouldContain "concurrency"
      }

      "Can create ServiceTagEngineConfig through YAML without serviceName" {
        val config = shouldNotThrowAny {
          ServiceTagEngineConfig.fromYamlString(
              """
concurrency: 10
          """,
          )
        }

        config.shouldBeInstanceOf<ServiceTagEngineConfig>()
        config.concurrency shouldBe 10
      }

      "Can create ServiceTagEngineConfig through YAML with all parameters" {
        val config = shouldNotThrowAny {
          ServiceTagEngineConfig.fromYamlString(
              """
concurrency: 10
storage:
  inMemory:
          """,
          )
        }

        config.shouldBeInstanceOf<ServiceTagEngineConfig>()
        config.concurrency shouldBe 10
        config.storage shouldBe storage
      }
    },
)
