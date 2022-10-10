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

package io.infinitic.common.workers

import io.infinitic.common.exceptions.thisShouldNotHappen
import io.infinitic.tasks.Retryable
import kotlin.math.min
import kotlin.math.pow
import kotlin.random.Random

sealed class RetryPolicyAbstract(
    open val maximumAttempts: Int,
    open val nonRetryableExceptions: List<String>
) : Retryable {

    val nonRetryableClasses: List<Class<*>> by lazy {
        nonRetryableExceptions.map { klass ->
            try {
                Class.forName(klass)
            } catch (e: ClassNotFoundException) {
                throw IllegalArgumentException("Unknown class \"$klass\" in ${::nonRetryableExceptions.name}")
            } catch (e: Exception) {
                throw IllegalArgumentException("Error with class \"$klass\" in ${::nonRetryableExceptions.name}", e)
            }.also {
                require(Exception::class.java.isAssignableFrom(it)) {
                    "Class \"$klass\" in ${::nonRetryableExceptions.name} must be an Exception"
                }
            }
        }
    }

    /**
     * Return how many seconds to wait before retrying
     * - attempt: current attempt (first = 1)
     * - exception: current Exception
     * Do not retry if return null
     */
    @Suppress("unused")
    override fun getSecondsBeforeRetry(attempt: Int, exception: Exception): Double? {
        // check that attempt is >= 1
        if (attempt < 1) thisShouldNotHappen()
        // check if we reached the maximal number of attempts
        if (attempt > maximumAttempts) return null
        // check if the exception is of a non retryable type
        if (nonRetryableClasses.any { it.isAssignableFrom(exception::class.java) }) return null

        return getSecondsBeforeRetryAttempt(attempt)
    }

    abstract fun check()

    protected abstract fun getSecondsBeforeRetryAttempt(attempt: Int): Double?
}

data class RetryExponentialBackoff(
    val initialIntervalSeconds: Double = 1.0,
    val backoffCoefficient: Double = 2.0,
    val maximumSeconds: Double = 100 * initialIntervalSeconds,
    override val maximumAttempts: Int = Int.MAX_VALUE,
    override val nonRetryableExceptions: List<String> = listOf()
) : RetryPolicyAbstract(maximumAttempts, nonRetryableExceptions) {

    // checks can not be in init {} as throwing exception in constructor prevents sealed class recognition by Hoplite
    override fun check() {
        require(initialIntervalSeconds > 0) { "${::initialIntervalSeconds.name} MUST be > 0" }
        require(backoffCoefficient > 0) { "${::backoffCoefficient.name} MUST be > 0" }
        require(maximumSeconds > 0) { "${::maximumSeconds.name} MUST be > 0" }
        require(maximumAttempts >= 0) { "${::maximumAttempts.name} MUST be >= 0" }

        // trigger checking of nonRetryableExceptions
        nonRetryableClasses
    }

    override fun getSecondsBeforeRetryAttempt(attempt: Int): Double =
        min(maximumSeconds, initialIntervalSeconds * (backoffCoefficient.pow(attempt - 1))) * Random.nextDouble()
}
