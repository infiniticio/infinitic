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
package io.infinitic.tasks

fun interface WithTimeout {
  /**
   * Returns the timeout duration in seconds.
   * Null means no timeout is set.
   */
  fun getTimeoutSeconds(): Double?

  /**
   * Returns the grace period after the timeout in seconds.
   * Default is 0 (no grace period).
   */
  fun getGracePeriodAfterTimeoutSeconds(): Double = 0.0
}

val WithTimeout.timeoutMillis: Result<Long?>
  get() = runCatching { getTimeoutSeconds()?.times(1000)?.toLong()?.coerceAtLeast(0L) }

val WithTimeout.graceMillis: Result<Long>
  get() = runCatching { getGracePeriodAfterTimeoutSeconds().times(1000).toLong().coerceAtLeast(0L) }

val UNSET_WITH_TIMEOUT: WithTimeout = WithTimeout { null }
