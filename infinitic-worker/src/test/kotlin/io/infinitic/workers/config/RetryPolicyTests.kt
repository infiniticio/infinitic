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

package io.infinitic.workers.config

import com.sksamuel.hoplite.ConfigException
import io.infinitic.common.config.loadConfigFromYaml
import io.infinitic.common.workers.config.RetryExponentialBackoff
import io.infinitic.workers.register.WorkerRegister.Companion.DEFAULT_TASK_TIMEOUT
import io.infinitic.workers.register.WorkerRegister.Companion.DEFAULT_WORKFLOW_TIMEOUT
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain

class RetryPolicyTests : StringSpec({

    "initialDelayInSeconds must be > 0" {
        val e = shouldThrow<IllegalArgumentException> {
            RetryExponentialBackoff(initialDelayInSeconds = 0.0).check()
        }
        e.message!! shouldContain RetryExponentialBackoff::initialDelayInSeconds.name

        val f = shouldThrow<IllegalArgumentException> {
            RetryExponentialBackoff(initialDelayInSeconds = -1.0).check()
        }
        f.message!! shouldContain RetryExponentialBackoff::initialDelayInSeconds.name
    }

    "backoffCoefficient can not be > 0" {
        val e = shouldThrow<IllegalArgumentException> {
            RetryExponentialBackoff(backoffCoefficient = 0.0).check()
        }
        e.message!! shouldContain RetryExponentialBackoff::backoffCoefficient.name

        val f = shouldThrow<IllegalArgumentException> {
            RetryExponentialBackoff(backoffCoefficient = -1.0).check()
        }
        f.message!! shouldContain RetryExponentialBackoff::backoffCoefficient.name
    }

    "maximumSeconds can not be > 0" {
        val e = shouldThrow<IllegalArgumentException> {
            RetryExponentialBackoff(maximumSeconds = 0.0).check()
        }
        e.message!! shouldContain RetryExponentialBackoff::maximumSeconds.name

        val f = shouldThrow<IllegalArgumentException> {
            RetryExponentialBackoff(maximumSeconds = -1.0).check()
        }
        f.message!! shouldContain RetryExponentialBackoff::maximumSeconds.name
    }

    "maximumAttempts can not be >= 0" {
        val f = shouldThrow<IllegalArgumentException> {
            RetryExponentialBackoff(maximumRetries = -1).check()
        }
        f.message!! shouldContain RetryExponentialBackoff::maximumRetries.name
    }

    "Unknown class in nonRetryableExceptions should throw" {
        val e = shouldThrow<ConfigException> {
            loadConfigFromYaml<WorkerConfig>(
                """
transport: inMemory
retry:
    nonRetryableExceptions:
        - foobar
"""
            )
        }
        e.message!! shouldContain "Unknown class \"foobar\""
    }

    "Unknown class in nonRetryableExceptions in task should throw" {
        val e = shouldThrow<ConfigException> {
            loadConfigFromYaml<WorkerConfig>(
                """
transport: inMemory
services:
    - name: io.infinitic.workers.samples.ServiceA
      class: io.infinitic.workers.samples.ServiceAImpl
      retry:
        nonRetryableExceptions:
          - foobar
"""
            )
        }
        e.message!! shouldContain "Unknown class \"foobar\""
    }

    "No Exception class in nonRetryableExceptions should throw" {
        val e = shouldThrow<ConfigException> {
            loadConfigFromYaml<WorkerConfig>(
                """
transport: inMemory
retry:
    nonRetryableExceptions:
        - io.infinitic.workers.InfiniticWorker
"""
            )
        }
        e.message!! shouldContain "\"io.infinitic.workers.InfiniticWorker\" in nonRetryableExceptions must be an Exception"
    }

    "No Exception class in nonRetryableExceptions in task should throw" {
        val e = shouldThrow<ConfigException> {
            loadConfigFromYaml<WorkerConfig>(
                """
transport: inMemory
services:
    - name: io.infinitic.workers.samples.ServiceA
      class: io.infinitic.workers.samples.ServiceAImpl
      retry:
        nonRetryableExceptions:
          - io.infinitic.workers.InfiniticWorker
"""
            )
        }
        e.message!! shouldContain "\"io.infinitic.workers.InfiniticWorker\" in nonRetryableExceptions must be an Exception"
    }

    "timeout in task should be positive" {
        val e = shouldThrow<ConfigException> {
            loadConfigFromYaml<WorkerConfig>(
                """
transport: inMemory
services:
    - name: io.infinitic.workers.samples.ServiceA
      class: io.infinitic.workers.samples.ServiceAImpl
      timeoutInSeconds: 0
"""
            )
        }
        e.message!! shouldContain "timeoutInSeconds"
    }

    "no timeout in service should be default" {
        val config = loadConfigFromYaml<WorkerConfig>(
            """
transport: inMemory
services:
    - name: io.infinitic.workers.samples.ServiceA
      class: io.infinitic.workers.samples.ServiceAImpl
"""
        )
        config.services.size shouldBe 1
        config.services[0].timeoutInSeconds shouldBe DEFAULT_TASK_TIMEOUT
    }

    "null timeout in service should not be default" {
        val config = loadConfigFromYaml<WorkerConfig>(
            """
transport: inMemory
services:
    - name: io.infinitic.workers.samples.ServiceA
      class: io.infinitic.workers.samples.ServiceAImpl
      timeoutInSeconds: null
"""
        )
        config.services.size shouldBe 1
        config.services[0].timeoutInSeconds shouldBe null
    }

    "no timeout in workflow should be default" {
        val config = loadConfigFromYaml<WorkerConfig>(
            """
transport: inMemory
workflows:
    - name: io.infinitic.workers.samples.WorkflowA
      class: io.infinitic.workers.samples.WorkflowAImpl
"""
        )
        config.workflows.size shouldBe 1
        config.workflows[0].timeoutInSeconds shouldBe DEFAULT_WORKFLOW_TIMEOUT.getTimeoutInSeconds()
    }

    "null timeout in workflow should be null" {
        val config = loadConfigFromYaml<WorkerConfig>(
            """
transport: inMemory
workflows:
    - name: io.infinitic.workers.samples.WorkflowA
      class: io.infinitic.workers.samples.WorkflowAImpl
      timeoutInSeconds: null
"""
        )
        config.workflows.size shouldBe 1
        config.workflows[0].timeoutInSeconds shouldBe null
    }

    "do not retry if maximumRetries = 0" {
        val workerConfig = loadConfigFromYaml<WorkerConfig>(
            """
transport: inMemory
retry:
    maximumRetries: 0
"""
        )

        workerConfig.retry shouldNotBe null
        workerConfig.retry!!.getSecondsBeforeRetry(0, Exception()) shouldBe null
    }

    "do not retry once reach maximumRetries" {
        val workerConfig = loadConfigFromYaml<WorkerConfig>(
            """
transport: inMemory
retry:
    maximumRetries: 10
"""
        )

        workerConfig.retry shouldNotBe null
        workerConfig.retry!!.getSecondsBeforeRetry(9, Exception()) shouldNotBe null
        workerConfig.retry!!.getSecondsBeforeRetry(10, Exception()) shouldBe null
    }

    "do not retry for non retryable exception" {
        val workerConfig = loadConfigFromYaml<WorkerConfig>(
            """
transport: inMemory
retry:
    nonRetryableExceptions:
        - io.infinitic.workers.config.TestException
"""
        )

        workerConfig.retry shouldNotBe null
        workerConfig.retry!!.getSecondsBeforeRetry(1, Exception()) shouldNotBe null
        workerConfig.retry!!.getSecondsBeforeRetry(1, TestException()) shouldBe null
        workerConfig.retry!!.getSecondsBeforeRetry(1, ChildTestException()) shouldBe null
    }
})

private open class TestException : Exception()

private class ChildTestException : TestException()
