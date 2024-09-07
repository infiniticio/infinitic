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

import com.sksamuel.hoplite.ConfigException
import io.infinitic.workers.samples.ServiceA
import io.infinitic.workers.samples.ServiceAImpl
import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.shouldBeInstanceOf

internal class ServiceConfigTests : StringSpec(
    {
      val serviceName = ServiceA::class.java.name
      val serviceClass = ServiceAImpl::class.java.name

      "Can create ServiceConfig through YAML with an executor" {
        val config = shouldNotThrowAny {
          ServiceConfig.fromYamlString(
              """
name: $serviceName
executor:
  class: $serviceClass
  concurrency: 10
          """,
          )
        }

        config.name shouldBe serviceName
        config.executor.shouldBeInstanceOf<ServiceExecutorConfig>()
        config.tagEngine shouldBe null
      }

      "Can create ServiceConfig through YAML with a Tag Engine" {
        val config = shouldNotThrowAny {
          ServiceConfig.fromYamlString(
              """
name: $serviceName
tagEngine:
  concurrency: 10
          """,
          )
        }

        config.name shouldBe serviceName
        config.executor shouldBe null
        config.tagEngine.shouldBeInstanceOf<ServiceTagEngineConfig>()
      }

      "Can create ServiceConfig through YAML with both an Executor and a Tag Engine" {
        val config = shouldNotThrowAny {
          ServiceConfig.fromYamlString(
              """
name: $serviceName
executor:
  class: $serviceClass
  concurrency: 10
tagEngine:
  concurrency: 10
          """,
          )
        }

        config.name shouldBe serviceName
        config.executor.shouldBeInstanceOf<ServiceExecutorConfig>()
        config.tagEngine.shouldBeInstanceOf<ServiceTagEngineConfig>()
      }

      "class must implements the Service" {
        val e = shouldThrow<ConfigException> {
          ServiceConfig.fromYamlString(
              """
name: UnknownService
executor:
  class: $serviceClass
          """,
          )
        }
        e.message shouldContain "'$serviceClass' must be an implementation of Service 'UnknownService'"
      }
    },
)
