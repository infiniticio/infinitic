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
import io.infinitic.common.config.loadConfigFromYaml
import io.infinitic.common.workers.config.ExponentialBackoffRetryPolicy
import io.infinitic.workflows.WorkflowCheckMode
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain

internal class RetryPolicyTests :
  StringSpec(
      {
        "initialDelayInSeconds must be > 0" {
          val e =
              shouldThrow<IllegalArgumentException> {
                ExponentialBackoffRetryPolicy(minimumSeconds = 0.0).check()
              }
          e.message!! shouldContain ExponentialBackoffRetryPolicy::minimumSeconds.name

          val f =
              shouldThrow<IllegalArgumentException> {
                ExponentialBackoffRetryPolicy(minimumSeconds = -1.0).check()
              }
          f.message!! shouldContain ExponentialBackoffRetryPolicy::minimumSeconds.name
        }

        "backoffCoefficient can not be > 0" {
          val e =
              shouldThrow<IllegalArgumentException> {
                ExponentialBackoffRetryPolicy(backoffCoefficient = 0.0).check()
              }
          e.message!! shouldContain ExponentialBackoffRetryPolicy::backoffCoefficient.name

          val f =
              shouldThrow<IllegalArgumentException> {
                ExponentialBackoffRetryPolicy(backoffCoefficient = -1.0).check()
              }
          f.message!! shouldContain ExponentialBackoffRetryPolicy::backoffCoefficient.name
        }

        "maximumSeconds can not be > 0" {
          val e =
              shouldThrow<IllegalArgumentException> {
                ExponentialBackoffRetryPolicy(maximumSeconds = 0.0).check()
              }
          e.message!! shouldContain ExponentialBackoffRetryPolicy::maximumSeconds.name

          val f =
              shouldThrow<IllegalArgumentException> {
                ExponentialBackoffRetryPolicy(maximumSeconds = -1.0).check()
              }
          f.message!! shouldContain ExponentialBackoffRetryPolicy::maximumSeconds.name
        }

        "maximumRetries can not be >= 0" {
          val f =
              shouldThrow<IllegalArgumentException> {
                ExponentialBackoffRetryPolicy(maximumRetries = -1).check()
              }
          f.message!! shouldContain ExponentialBackoffRetryPolicy::maximumRetries.name
        }

        "Unknown class in ignoredExceptions should throw" {
          val e =
              shouldThrow<ConfigException> {
                loadConfigFromYaml<WorkerConfig>(
                    """
transport: inMemory
serviceDefault:
    retry:
        ignoredExceptions:
            - foobar
""",
                )
              }
          e.message!! shouldContain "Class 'foobar' not found"
        }

        "Unknown class in ignoredExceptions in task should throw" {
          val e =
              shouldThrow<ConfigException> {
                loadConfigFromYaml<WorkerConfig>(
                    """
transport: inMemory
services:
    - name: io.infinitic.workers.samples.ServiceA
      class: io.infinitic.workers.samples.ServiceAImpl
      retry:
        ignoredExceptions:
          - foobar
""",
                )
              }
          e.message!! shouldContain "Class 'foobar' not found"
        }

        "No Exception class in ignoredExceptions should throw" {
          val e =
              shouldThrow<ConfigException> {
                loadConfigFromYaml<WorkerConfig>(
                    """
transport: inMemory
serviceDefault:
  retry:
    ignoredExceptions:
      - io.infinitic.workers.InfiniticWorker
""",
                )
              }
          e.message!! shouldContain
              "'io.infinitic.workers.InfiniticWorker' in ignoredExceptions must be an Exception"
        }

        "No Exception class in ignoredExceptions in task should throw" {
          val e =
              shouldThrow<ConfigException> {
                loadConfigFromYaml<WorkerConfig>(
                    """
transport: inMemory
services:
    - name: io.infinitic.workers.samples.ServiceA
      class: io.infinitic.workers.samples.ServiceAImpl
      retry:
        ignoredExceptions:
          - io.infinitic.workers.InfiniticWorker
""",
                )
              }
          e.message!! shouldContain
              "'io.infinitic.workers.InfiniticWorker' in ignoredExceptions must be an Exception"
        }

        "timeout in task should be positive" {
          val e =
              shouldThrow<ConfigException> {
                loadConfigFromYaml<WorkerConfig>(
                    """
transport: inMemory
services:
    - name: io.infinitic.workers.samples.ServiceA
      class: io.infinitic.workers.samples.ServiceAImpl
      timeoutInSeconds: 0
""",
                )
              }
          e.message!! shouldContain "timeoutInSeconds"
        }

        "checking default for service" {
          val config =
              loadConfigFromYaml<WorkerConfig>(
                  """
transport: inMemory
services:
    - name: io.infinitic.workers.samples.ServiceA
      class: io.infinitic.workers.samples.ServiceAImpl
""",
              )
          config.services.size shouldBe 1
          config.services[0].timeoutInSeconds shouldBe null
          config.services[0].retry shouldBe null
          config.services[0].concurrency shouldBe 1
        }

        "null timeout in service should not be default" {
          val config =
              loadConfigFromYaml<WorkerConfig>(
                  """
transport: inMemory
services:
    - name: io.infinitic.workers.samples.ServiceA
      class: io.infinitic.workers.samples.ServiceAImpl
      timeoutInSeconds: null
""",
              )
          config.services.size shouldBe 1
          config.services[0].timeoutInSeconds shouldBe null
        }

        "get timeout in service from default" {
          val config =
              loadConfigFromYaml<WorkerConfig>(
                  """
transport: inMemory
serviceDefault:
  timeoutInSeconds: 1
services:
    - name: io.infinitic.workers.samples.ServiceA
      class: io.infinitic.workers.samples.ServiceAImpl
""",
              )
          config.services.size shouldBe 1
          config.services[0].timeoutInSeconds shouldBe 1
        }

        "get retry in service from default" {
          val config =
              loadConfigFromYaml<WorkerConfig>(
                  """
transport: inMemory
serviceDefault:
  retry:
    maximumRetries: 42
services:
    - name: io.infinitic.workers.samples.ServiceA
      class: io.infinitic.workers.samples.ServiceAImpl
""",
              )
          config.services.size shouldBe 1
          config.services[0].retry?.maximumRetries shouldBe 42
        }

        "checking default in workflow" {
          val config =
              loadConfigFromYaml<WorkerConfig>(
                  """
transport: inMemory
workflows:
    - name: io.infinitic.workers.samples.WorkflowA
      class: io.infinitic.workers.samples.WorkflowAImpl
""",
              )
          config.workflows.size shouldBe 1
          config.workflows[0].timeoutInSeconds shouldBe null
          config.workflows[0].retry shouldBe null
          config.workflows[0].concurrency shouldBe 1
          config.workflows[0].checkMode shouldBe null
        }

        "get workflow timeout from default" {
          val config =
              loadConfigFromYaml<WorkerConfig>(
                  """
transport: inMemory
workflowDefault:
  timeoutInSeconds: 1
workflows:
    - name: io.infinitic.workers.samples.WorkflowA
      class: io.infinitic.workers.samples.WorkflowAImpl
""",
              )
          config.workflows.size shouldBe 1
          config.workflows[0].timeoutInSeconds shouldBe 1
        }

        "get workflow retry from default" {
          val config =
              loadConfigFromYaml<WorkerConfig>(
                  """
transport: inMemory
workflowDefault:
  retry:
    maximumRetries: 42
workflows:
    - name: io.infinitic.workers.samples.WorkflowA
      class: io.infinitic.workers.samples.WorkflowAImpl
""",
              )
          config.workflows.size shouldBe 1
          config.workflows[0].retry?.maximumRetries shouldBe 42
        }

        "get workflow checkmode from default" {
          val config =
              loadConfigFromYaml<WorkerConfig>(
                  """
transport: inMemory
workflowDefault:
  checkMode: strict
workflows:
    - name: io.infinitic.workers.samples.WorkflowA
      class: io.infinitic.workers.samples.WorkflowAImpl
""",
              )
          config.workflows.size shouldBe 1
          config.workflows[0].checkMode shouldBe WorkflowCheckMode.strict
        }

        "do not retry if maximumRetries = 0" {
          val workerConfig =
              loadConfigFromYaml<WorkerConfig>(
                  """
transport: inMemory
serviceDefault:
  retry:
    maximumRetries: 0
""",
              )
          workerConfig.serviceDefault.retry shouldNotBe null
          workerConfig.serviceDefault.retry!!.getSecondsBeforeRetry(0, Exception()) shouldBe null
        }

        "do not retry once reach maximumRetries" {
          val workerConfig =
              loadConfigFromYaml<WorkerConfig>(
                  """
transport: inMemory
serviceDefault:
  retry:
    maximumRetries: 10
""",
              )
          workerConfig.serviceDefault.retry shouldNotBe null
          workerConfig.serviceDefault.retry!!.getSecondsBeforeRetry(9, Exception()) shouldNotBe null
          workerConfig.serviceDefault.retry!!.getSecondsBeforeRetry(10, Exception()) shouldBe null
        }

        "do not retry for non retryable exception" {
          val workerConfig =
              loadConfigFromYaml<WorkerConfig>(
                  """
transport: inMemory
serviceDefault:
  retry:
    ignoredExceptions:
        - io.infinitic.workers.config.TestException
""",
              )
          workerConfig.serviceDefault.retry shouldNotBe null
          workerConfig.serviceDefault.retry!!.getSecondsBeforeRetry(1, Exception()) shouldNotBe null
          workerConfig.serviceDefault.retry!!.getSecondsBeforeRetry(
              1,
              TestException(),
          ) shouldBe null
          workerConfig.serviceDefault.retry!!.getSecondsBeforeRetry(
              1,
              ChildTestException(),
          ) shouldBe null
        }
      },
  )

private open class TestException : Exception()

private class ChildTestException : TestException()
