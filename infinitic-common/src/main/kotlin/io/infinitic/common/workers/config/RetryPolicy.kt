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
package io.infinitic.common.workers.config

import io.infinitic.common.exceptions.thisShouldNotHappen
import io.infinitic.common.utils.getClass
import io.infinitic.tasks.WithRetry
import io.infinitic.tasks.WithRetryBuilder
import kotlin.math.min
import kotlin.math.pow
import kotlin.random.Random

sealed class RetryPolicy(open val maximumRetries: Int, open val ignoredExceptions: List<String>) :
  WithRetry {

  val ignoredClasses: List<Class<*>> by lazy {
    ignoredExceptions.map { klass ->
      klass.getClass().getOrThrow().also {
        require(Exception::class.java.isAssignableFrom(it)) {
          "Class '$klass' in ${::ignoredExceptions.name} must be an Exception"
        }
      }
    }
  }

  /**
   * Return how many seconds to wait before retrying
   * - attempt: current attempt (first = 0)
   * - exception: current Exception Do not retry if return null
   */
  @Suppress("unused")
  override fun getSecondsBeforeRetry(retry: Int, e: Exception): Double? {
    // check that attempt is >= 1
    if (retry < 0) thisShouldNotHappen()
    // check if we reached the maximal number of attempts
    if (retry >= maximumRetries) return null
    // check if the exception is of a non retryable type
    if (ignoredClasses.any { it.isAssignableFrom(e::class.java) }) return null

    return getSecondsBeforeRetry(retry)
  }

  abstract fun check()

  protected abstract fun getSecondsBeforeRetry(attempt: Int): Double?
}

@Suppress("ClassName")
data object UNSET_RETRY_POLICY : RetryPolicy(0, listOf()) {
  override fun check() {}
  override fun getSecondsBeforeRetry(attempt: Int) = -Double.MAX_VALUE
}

@Suppress("unused")
data class WithExponentialBackoffRetry(
  val minimumSeconds: Double = 1.0,
  val maximumSeconds: Double = 1000 * minimumSeconds,
  val backoffCoefficient: Double = 2.0,
  val randomFactor: Double = 0.5,
  override val maximumRetries: Int = 11,
  override val ignoredExceptions: List<String> = listOf()
) : RetryPolicy(maximumRetries, ignoredExceptions) {

  // checks can not be in init {} as throwing exception in constructor
  // prevents sealed class recognition by Hoplite
  override fun check() {
    require(minimumSeconds > 0) { "${::minimumSeconds.name} MUST be > 0" }
    require(maximumSeconds >= 0) { "${::maximumSeconds.name} MUST be > 0" }
    require(maximumSeconds > minimumSeconds) {
      "${::maximumSeconds.name} MUST be > ${::minimumSeconds.name}"
    }
    require(backoffCoefficient >= 1) { "${::backoffCoefficient.name} MUST be >= 1" }
    require(maximumRetries >= 0) { "${::maximumRetries.name} MUST be >= 0" }
    require(randomFactor >= 0) { "${::randomFactor.name} MUST be >= 0" }
    require(randomFactor <= 1) { "${::randomFactor.name} MUST be <= 1" }

    // trigger checking of nonRetryableExceptions
    ignoredClasses
  }

  override fun getSecondsBeforeRetry(attempt: Int): Double =
      min(maximumSeconds, minimumSeconds * backoffCoefficient.pow(attempt)) *
          (1 + randomFactor * (2 * Random.nextDouble() - 1))

  companion object {
    @JvmStatic
    fun builder() = ExponentialBackoffRetryPolicyBuilder()
  }

  class ExponentialBackoffRetryPolicyBuilder : WithRetryBuilder {
    private val default = WithExponentialBackoffRetry()
    private var minimumSeconds = default.minimumSeconds
    private var maximumSeconds = default.maximumSeconds
    private var backoffCoefficient = default.backoffCoefficient
    private var randomFactor = default.randomFactor
    private var maximumRetries = default.maximumRetries
    private var ignoredExceptions = default.ignoredExceptions as MutableList<String>

    override fun build() = WithExponentialBackoffRetry(
        minimumSeconds,
        maximumSeconds,
        backoffCoefficient,
        randomFactor,
        maximumRetries,
        ignoredExceptions,
    )

    fun setMinimumSeconds(minimumSeconds: Double) =
        apply { this.minimumSeconds = minimumSeconds }

    fun setMaximumSeconds(maximumSeconds: Double) =
        apply { this.maximumSeconds = maximumSeconds }

    fun setBackoffCoefficient(backoffCoefficient: Double) =
        apply { this.backoffCoefficient = backoffCoefficient }

    fun setRandomFactor(randomFactor: Double) =
        apply { this.randomFactor = randomFactor }

    fun setMaximumRetries(maximumRetries: Int) =
        apply { this.maximumRetries = maximumRetries }

    fun addIgnoredException(exception: Class<Exception>) =
        apply { ignoredExceptions.add(exception.name) }
  }
}
