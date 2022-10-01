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
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain

class RetryPolicyTests : StringSpec({

    "initialIntervalSeconds must be > 0" {
        val e = shouldThrow<IllegalArgumentException> {
            RetryExponentialBackoff(initialIntervalSeconds = 0.0).check()
        }
        e.message!! shouldContain "initialIntervalSeconds"

        val f = shouldThrow<IllegalArgumentException> {
            RetryExponentialBackoff(initialIntervalSeconds = -1.0).check()
        }
        f.message!! shouldContain "initialIntervalSeconds"
    }

    "backoffCoefficient can not be > 0" {
        val e = shouldThrow<IllegalArgumentException> {
            RetryExponentialBackoff(backoffCoefficient = 0.0).check()
        }
        e.message!! shouldContain "backoffCoefficient"

        val f = shouldThrow<IllegalArgumentException> {
            RetryExponentialBackoff(backoffCoefficient = -1.0).check()
        }
        f.message!! shouldContain "backoffCoefficient"
    }

    "maximumSeconds can not be > 0" {
        val e = shouldThrow<IllegalArgumentException> {
            RetryExponentialBackoff(maximumSeconds = 0.0).check()
        }
        e.message!! shouldContain "maximumSeconds"

        val f = shouldThrow<IllegalArgumentException> {
            RetryExponentialBackoff(maximumSeconds = -1.0).check()
        }
        f.message!! shouldContain "maximumSeconds"
    }

    "maximumAttempts can not be >= 0" {
        val f = shouldThrow<IllegalArgumentException> {
            RetryExponentialBackoff(maximumAttempts = -1).check()
        }
        f.message!! shouldContain "maximumAttempts"
    }

    "Unknown class in nonRetryableExceptions should throw" {
        val e = shouldThrow<ConfigException> {
            loadConfigFromYaml<WorkerConfig>(
                """
transport: inMemory
retryPolicy:
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
tasks:
    - name: io.infinitic.workers.samples.TaskA
      class: io.infinitic.workers.samples.TaskAImpl
      retryPolicy:
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
retryPolicy:
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
tasks:
    - name: io.infinitic.workers.samples.TaskA
      class: io.infinitic.workers.samples.TaskAImpl
      retryPolicy:
        nonRetryableExceptions:
          - io.infinitic.workers.InfiniticWorker
     """
            )
        }
        e.message!! shouldContain "\"io.infinitic.workers.InfiniticWorker\" in nonRetryableExceptions must be an Exception"
    }

    "do not retry if maximumAttempts = 0" {
        val workerConfig = loadConfigFromYaml<WorkerConfig>(
            """
transport: inMemory
retryPolicy:
    maximumAttempts: 0
        """
        )

        workerConfig.retryPolicy.getSecondsBeforeRetry(1, Exception()) shouldBe null
    }

    "do not retry once reach maximumAttempts" {
        val workerConfig = loadConfigFromYaml<WorkerConfig>(
            """
transport: inMemory
retryPolicy:
    maximumAttempts: 10
        """
        )

        workerConfig.retryPolicy.getSecondsBeforeRetry(10, Exception()) shouldNotBe null
        workerConfig.retryPolicy.getSecondsBeforeRetry(11, Exception()) shouldBe null
    }

    "do not retry for non retryable exception" {
        val workerConfig = loadConfigFromYaml<WorkerConfig>(
            """
transport: inMemory
retryPolicy:
    nonRetryableExceptions:
        - io.infinitic.workers.config.TestException
        """
        )

        workerConfig.retryPolicy.getSecondsBeforeRetry(1, Exception()) shouldNotBe null
        workerConfig.retryPolicy.getSecondsBeforeRetry(1, TestException()) shouldBe null
        workerConfig.retryPolicy.getSecondsBeforeRetry(1, ChildTestException()) shouldBe null
    }
})

private open class TestException : Exception()

private class ChildTestException : TestException()
