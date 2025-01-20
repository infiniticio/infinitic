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
@file:Suppress("ClassName")

package io.infinitic.common.workers.config

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain

class RetryPolicyTests :
  StringSpec(
      {
        "minimumSeconds should be >=0" {
          val e =
              shouldThrow<IllegalArgumentException> {
                WithExponentialBackoffRetry(minimumSeconds = -1.0).check()
              }

          e.message shouldContain "minimumSeconds"
        }

        "maximumSeconds should be >0" {
          val e =
              shouldThrow<IllegalArgumentException> {
                WithExponentialBackoffRetry(maximumSeconds = 0.0).check()
              }

          e.message shouldContain "maximumSeconds"
        }

        "maximumSeconds should be > minimumSeconds" {
          val e =
              shouldThrow<IllegalArgumentException> {
                WithExponentialBackoffRetry(minimumSeconds = 4.0, maximumSeconds = 3.0).check()
              }

          e.message shouldContain "minimumSeconds"
          e.message shouldContain "maximumSeconds"
        }

        "backoffCoefficient should be >= 1" {
          val e =
              shouldThrow<IllegalArgumentException> {
                WithExponentialBackoffRetry(backoffCoefficient = 0.9).check()
              }

          e.message shouldContain "backoffCoefficient"
        }

        "maximumRetries should be >= 0" {
          val e =
              shouldThrow<IllegalArgumentException> {
                WithExponentialBackoffRetry(maximumRetries = -1).check()
              }

          e.message shouldContain "maximumRetries"
        }

        "randomFactor should be >= 0" {
          val e =
              shouldThrow<IllegalArgumentException> {
                WithExponentialBackoffRetry(randomFactor = -1.0).check()
              }

          e.message shouldContain "randomFactor"
        }

        "randomFactor should be < 1" {
          val e =
              shouldThrow<IllegalArgumentException> {
                WithExponentialBackoffRetry(randomFactor = 1.1).check()
              }

          e.message shouldContain "randomFactor"
        }

        "should no retry after maximumRetries" {
          val p = WithExponentialBackoffRetry(maximumRetries = 10)

          p.getSecondsBeforeRetry(9, Exception()) shouldNotBe null
          p.getSecondsBeforeRetry(10, Exception()) shouldBe null
        }
      },
  )
