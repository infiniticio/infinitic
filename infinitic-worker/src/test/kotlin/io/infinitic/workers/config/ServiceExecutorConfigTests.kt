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
import io.infinitic.common.workers.config.WithExponentialBackoffRetry
import io.infinitic.tasks.WithRetry
import io.infinitic.tasks.WithTimeout
import io.infinitic.workers.samples.ServiceA
import io.infinitic.workers.samples.ServiceAImpl
import io.infinitic.workers.samples.ServiceWithExceptionInInitializerError
import io.infinitic.workers.samples.ServiceWithInvocationTargetException
import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.shouldBeInstanceOf

internal class ServiceExecutorConfigTests : StringSpec(
    {
      val serviceName = ServiceA::class.java.name

      "Can create ServiceExecutorConfig through builder with default parameters" {
        val config = shouldNotThrowAny {
          ServiceExecutorConfig.builder()
              .setServiceName(serviceName)
              .setFactory { ServiceAImpl() }
              .build()
        }

        config.shouldBeInstanceOf<ServiceExecutorConfig>()
        config.serviceName shouldBe serviceName
        config.factory?.invoke().shouldBeInstanceOf<ServiceAImpl>()
        config.concurrency shouldBe 1
        config.withRetry shouldBe WithRetry.UNSET
        config.withTimeout shouldBe WithTimeout.UNSET
      }

      "Can create ServiceExecutorConfig through builder with concurrency" {
        val config = shouldNotThrowAny {
          ServiceExecutorConfig.builder()
              .setServiceName(serviceName)
              .setConcurrency(10)
              .setFactory { ServiceAImpl() }
              .build()
        }

        config.shouldBeInstanceOf<ServiceExecutorConfig>()
        config.concurrency shouldBe 10
        config.retryHandlerConcurrency shouldBe 10
        config.eventHandlerConcurrency shouldBe 10
      }

      "Can create ServiceExecutorConfig through builder with all parameters" {
        val withRetry = WithExponentialBackoffRetry()
        val config = shouldNotThrowAny {
          ServiceExecutorConfig.builder()
              .setServiceName(serviceName)
              .setFactory { ServiceAImpl() }
              .setConcurrency(10)
              .setEventHandlerConcurrency(11)
              .setRetryHandlerConcurrency(12)
              .setTimeoutSeconds(3.0)
              .withRetry(withRetry)
              .build()
        }

        config.shouldBeInstanceOf<ServiceExecutorConfig>()
        config.factory?.invoke().shouldBeInstanceOf<ServiceAImpl>()
        config.concurrency shouldBe 10
        config.eventHandlerConcurrency shouldBe 11
        config.retryHandlerConcurrency shouldBe 12
        config.withRetry shouldBe withRetry
        config.withTimeout?.getTimeoutSeconds() shouldBe 3.0
      }

      "Can create ServiceExecutorConfig through builder with only retries" {
        WithExponentialBackoffRetry()
        val config = shouldNotThrowAny {
          ServiceExecutorConfig.builder()
              .setServiceName(serviceName)
              .setConcurrency(0)
              .setRetryHandlerConcurrency(10)
              .build()
        }

        config.shouldBeInstanceOf<ServiceExecutorConfig>()
        config.concurrency shouldBe 0
        config.retryHandlerConcurrency shouldBe 10
        config.eventHandlerConcurrency shouldBe 0
      }

      "Can create ServiceExecutorConfig through builder with only events" {
        WithExponentialBackoffRetry()
        val config = shouldNotThrowAny {
          ServiceExecutorConfig.builder()
              .setServiceName(serviceName)
              .setConcurrency(0)
              .setEventHandlerConcurrency(10)
              .build()
        }

        config.shouldBeInstanceOf<ServiceExecutorConfig>()
        config.concurrency shouldBe 0
        config.retryHandlerConcurrency shouldBe 0
        config.eventHandlerConcurrency shouldBe 10
      }

      "Concurrency must be positive when building ServiceExecutorConfig" {
        val e = shouldThrow<IllegalArgumentException> {
          ServiceExecutorConfig.builder()
              .setServiceName(serviceName)
              .setFactory { ServiceAImpl() }
              .setConcurrency(0)
              .build()
        }
        e.message shouldContain "concurrency"
      }

      "serviceName is mandatory when building ServiceExecutorConfig" {
        val e = shouldThrow<IllegalArgumentException> {
          ServiceExecutorConfig.builder()
              .setFactory { ServiceAImpl() }
              .build()
        }
        e.message shouldContain "serviceName"
      }

      "Factory is mandatory when building ServiceExecutorConfig" {
        val e = shouldThrow<IllegalArgumentException> {
          ServiceExecutorConfig.builder()
              .setServiceName(serviceName)
              .build()
        }
        e.message shouldContain "factory"
      }

      "Can create ServiceExecutorConfig through YAML with default parameters" {
        val config = shouldNotThrowAny {
          ServiceExecutorConfig.fromYamlString(
              """
            class: ${ServiceAImpl::class.java.name}
           """.trimIndent(),
          )
        }

        config.factory?.invoke().shouldBeInstanceOf<ServiceAImpl>()
        config.concurrency shouldBe 1
        config.withRetry shouldBe WithRetry.UNSET
        config.withTimeout shouldBe WithTimeout.UNSET
      }

      "Can create ServiceExecutorConfig through YAML with only retries" {
        val config = shouldNotThrowAny {
          ServiceExecutorConfig.fromYamlString(
              """
                concurrency: 0
                retryHandlerConcurrency: 1
          """.trimIndent(),
          )
        }

        config.factory shouldBe null
        config.concurrency shouldBe 0
        config.retryHandlerConcurrency shouldBe 1
        config.eventHandlerConcurrency shouldBe 0
      }

      "Can create ServiceExecutorConfig through YAML with only events" {
        val config = shouldNotThrowAny {
          ServiceExecutorConfig.fromYamlString(
              """
                concurrency: 0
                eventHandlerConcurrency: 1
          """.trimIndent(),
          )
        }

        config.factory shouldBe null
        config.concurrency shouldBe 0
        config.retryHandlerConcurrency shouldBe 0
        config.eventHandlerConcurrency shouldBe 1
      }

      "Can create ServiceExecutorConfig through YAML with concurrency" {
        WithExponentialBackoffRetry(minimumSeconds = 4.0)
        val config = shouldNotThrowAny {
          ServiceExecutorConfig.fromYamlString(
              """
              class: ${ServiceAImpl::class.java.name}
              concurrency: 10
          """.trimIndent(),
          )
        }

        config.factory?.invoke().shouldBeInstanceOf<ServiceAImpl>()
        config.concurrency shouldBe 10
        config.retryHandlerConcurrency shouldBe 10
        config.eventHandlerConcurrency shouldBe 10
        config.withTimeout shouldBe WithTimeout.UNSET
        config.withRetry shouldBe WithRetry.UNSET
      }

      "Can create ServiceExecutorConfig through YAML with all parameters" {
        val withRetry = WithExponentialBackoffRetry(minimumSeconds = 4.0)
        val config = shouldNotThrowAny {
          ServiceExecutorConfig.fromYamlString(
              """
              class: ${ServiceAImpl::class.java.name}
              concurrency: 10
              retryHandlerConcurrency: 11
              eventHandlerConcurrency: 12
              timeoutSeconds: 3.0
              retry:
                minimumSeconds: 4
          """.trimIndent(),
          )
        }

        config.factory?.invoke().shouldBeInstanceOf<ServiceAImpl>()
        config.concurrency shouldBe 10
        config.retryHandlerConcurrency shouldBe 11
        config.eventHandlerConcurrency shouldBe 12
        config.withTimeout?.getTimeoutSeconds() shouldBe 3.0
        config.withRetry shouldBe withRetry
      }

      "class is mandatory when building ServiceExecutorConfig from YAML" {
        val e = shouldThrow<ConfigException> {
          ServiceExecutorConfig.fromYamlString(
              """
              concurrency: 10
            """.trimIndent(),
          )
        }

        e.message shouldContain "class"
      }

      "Unknown class in ignoredExceptions should throw" {
        val e = shouldThrow<ConfigException> {
          ServiceExecutorConfig.fromYamlString(
              """
            class: ${ServiceAImpl::class.java.name}
            retry:
              ignoredExceptions:
                - foobar
            """.trimIndent(),
          )
        }
        e.message shouldContain "Class 'foobar' not found"
      }

      "Class not an Exception in ignoredExceptions should throw" {
        val e = shouldThrow<ConfigException> {
          ServiceExecutorConfig.fromYamlString(
              """
            class: ${ServiceAImpl::class.java.name}
            retry:
              ignoredExceptions:
                - ${ServiceA::class.java.name}
            """.trimIndent(),
          )
        }
        e.message shouldContain "must be an Exception"
      }

      "timeout must be > 0 when building ServiceExecutorConfig from YAML" {
        val e = shouldThrow<ConfigException> {
          ServiceExecutorConfig.fromYamlString(
              """
            class: ${ServiceAImpl::class.java.name}
            timeoutSeconds: 0
            """.trimIndent(),
          )
        }
        e.message shouldContain "timeoutSeconds must be > 0"
      }

      "task with InvocationTargetException should throw cause" {
        val e = shouldThrow<ConfigException> {
          ServiceExecutorConfig.fromYamlString(
              """
            class: ${ServiceWithInvocationTargetException::class.java.name}
            """.trimIndent(),
          )
        }
        e.message shouldContain
            "Error during class '${ServiceWithInvocationTargetException::class.java.name}' instantiation"
      }

      "task with ServiceWithExceptionInInitializerError should throw cause" {
        val e = shouldThrow<ConfigException> {
          ServiceExecutorConfig.fromYamlString(
              """
            class: ${ServiceWithExceptionInInitializerError::class.java.name}
            """.trimIndent(),
          )
        }
        e.message shouldContain "ExceptionInInitializerError"
      }

      "service Unknown" {
        val e = shouldThrow<ConfigException> {
          ServiceExecutorConfig.fromYamlString(
              """
            class: io.infinitic.workers.samples.UnknownService
            """.trimIndent(),
          )
        }
        e.message shouldContain "Class 'io.infinitic.workers.samples.UnknownService' not found"
      }
    },
)
