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

import io.infinitic.common.workers.config.ExponentialBackoffRetryPolicy
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.string.shouldContain

internal class RetryPolicyTests :
  StringSpec(
      {
        "initialDelayInSeconds must be > 0" {
          val e = shouldThrow<IllegalArgumentException> {
                ExponentialBackoffRetryPolicy(minimumSeconds = 0.0).check()
              }
          e.message!! shouldContain ExponentialBackoffRetryPolicy::minimumSeconds.name

          val f = shouldThrow<IllegalArgumentException> {
                ExponentialBackoffRetryPolicy(minimumSeconds = -1.0).check()
              }
          f.message!! shouldContain ExponentialBackoffRetryPolicy::minimumSeconds.name
        }

        "backoffCoefficient can not be > 0" {
          val e = shouldThrow<IllegalArgumentException> {
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
          val e = shouldThrow<IllegalArgumentException> {
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
          val f = shouldThrow<IllegalArgumentException> {
                ExponentialBackoffRetryPolicy(maximumRetries = -1).check()
              }
          f.message!! shouldContain ExponentialBackoffRetryPolicy::maximumRetries.name
        }
      },
  )

