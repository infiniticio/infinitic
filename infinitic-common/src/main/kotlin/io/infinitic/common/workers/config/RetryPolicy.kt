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

package io.infinitic.common.workers.config

import io.infinitic.common.exceptions.thisShouldNotHappen
import io.infinitic.common.utils.getClass
import io.infinitic.tasks.Retryable
import kotlin.math.min
import kotlin.math.pow
import kotlin.random.Random

sealed class RetryPolicy(
    open val maximumRetries: Int,
    open val nonRetryableExceptions: List<String>
) : Retryable {

    val nonRetryableClasses: List<Class<*>> by lazy {
        nonRetryableExceptions.map { klass ->
            klass.getClass(
                classNotFound = "Unknown class \"$klass\" in ${::nonRetryableExceptions.name}",
                errorClass = "Error with class \"$klass\" in ${::nonRetryableExceptions.name}"
            ).also {
                require(Exception::class.java.isAssignableFrom(it)) {
                    "Class \"$klass\" in ${::nonRetryableExceptions.name} must be an Exception"
                }
            }
        }
    }

    /**
     * Return how many seconds to wait before retrying
     * - attempt: current attempt (first = 0)
     * - exception: current Exception
     * Do not retry if return null
     */
    @Suppress("unused")
    override fun getSecondsBeforeRetry(retry: Int, exception: Exception): Double? {
        // check that attempt is >= 1
        if (retry < 0) thisShouldNotHappen()
        // check if we reached the maximal number of attempts
        if (retry >= maximumRetries) return null
        // check if the exception is of a non retryable type
        if (nonRetryableClasses.any { it.isAssignableFrom(exception::class.java) }) return null

        return getSecondsBeforeRetry(retry)
    }

    abstract fun check()

    protected abstract fun getSecondsBeforeRetry(attempt: Int): Double?
}

data class RetryExponentialBackoff(
    val initialIntervalSeconds: Double = 1.0,
    val backoffCoefficient: Double = 2.0,
    val maximumSeconds: Double = 100 * initialIntervalSeconds,
    val randomized: Boolean = true,
    override val maximumRetries: Int = Int.MAX_VALUE,
    override val nonRetryableExceptions: List<String> = listOf()
) : RetryPolicy(maximumRetries, nonRetryableExceptions) {

    // checks can not be in init {} as throwing exception in constructor prevents sealed class recognition by Hoplite
    override fun check() {
        require(initialIntervalSeconds > 0) { "${::initialIntervalSeconds.name} MUST be > 0" }
        require(backoffCoefficient > 0) { "${::backoffCoefficient.name} MUST be > 0" }
        require(maximumSeconds > 0) { "${::maximumSeconds.name} MUST be > 0" }
        require(maximumRetries >= 0) { "${::maximumRetries.name} MUST be >= 0" }

        // trigger checking of nonRetryableExceptions
        nonRetryableClasses
    }

    override fun getSecondsBeforeRetry(attempt: Int): Double =
        min(maximumSeconds, initialIntervalSeconds * (backoffCoefficient.pow(attempt))) * when (randomized) {
            true -> Random.nextDouble()
            false -> 1.0
        }
}
