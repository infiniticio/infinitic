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
package io.infinitic.dashboard.panels.infrastructure.requests

import org.apache.pulsar.client.admin.PulsarAdminException
import java.time.Instant

sealed class Request<T> {
  abstract val isLoading: Boolean
  abstract val lastUpdated: Instant

  abstract fun copyLoading(): Request<T>
}

data class Loading<T>(override val lastUpdated: Instant = Instant.now()) : Request<T>() {
  override val isLoading = true

  override fun copyLoading() = copy()
}

data class Completed<T>(
  val result: T,
  override val isLoading: Boolean = false,
  override val lastUpdated: Instant = Instant.now()
) : Request<T>() {
  override fun copyLoading() = copy(isLoading = true)
}

data class Failed<T>(
  val error: Throwable,
  override val isLoading: Boolean = false,
  override val lastUpdated: Instant = Instant.now()
) : Request<T>() {
  override fun copyLoading() = copy(isLoading = true)

  val title: String
    get() =
      when (error) {
        is PulsarAdminException.NotFoundException -> "not found!"
        is PulsarAdminException.NotAllowedException -> "not allowed!"
        is PulsarAdminException.NotAuthorizedException -> "not authorized!"
        is PulsarAdminException.TimeoutException -> "timed out!"
        else -> "error!"
      }
}
