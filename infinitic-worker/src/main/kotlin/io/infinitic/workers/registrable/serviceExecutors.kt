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
package io.infinitic.workers.registrable

import io.infinitic.common.tasks.data.ServiceName
import io.infinitic.tasks.WithRetry
import io.infinitic.tasks.WithTimeout

typealias ServiceFactory = () -> Any

internal class RegisteredServiceExecutor internal constructor(
  val serviceName: ServiceName,
  val concurrency: Int,
  val factory: ServiceFactory,
  val withTimeout: WithTimeout?,
  val withRetry: WithRetry?
) : Registered

class ServiceExecutor(
  var serviceName: String,
  var concurrency: Int? = null,
  var factory: ServiceFactory? = null,
  var withTimeout: WithTimeout? = null,
  var withRetry: WithRetry? = null
) : Registrable {

  fun constructor(serviceName: String) = ServiceExecutor(serviceName)

  fun concurrency(concurrency: Int) =
      apply { this.concurrency = concurrency }

  fun factory(factory: ServiceFactory) =
      apply { this.factory = factory }

  fun withTimeout(withTimeout: WithTimeout) =
      apply { this.withTimeout = withTimeout }

  fun withRetry(withRetry: WithRetry) =
      apply { this.withRetry = withRetry }
}

internal fun ServiceExecutor.applyDefault(default: ServiceDefault?): ServiceExecutor {
  if (default == null) return this
  if (concurrency == null) concurrency = default.concurrency
  if (withTimeout == null) withTimeout = default.withTimeout
  if (withRetry == null) withRetry = default.withRetry
  return this
}

internal fun ServiceExecutor.build(): Registered {
  require(serviceName.isNotBlank()) { "Service name must not be blank" }
  require(concurrency?.let { it > 0 } ?: false) { "Concurrency must be > 0" }
  requireNotNull(factory) { "Factory must be a function returning an instance of Service '$serviceName'" }

  return RegisteredServiceExecutor(
      ServiceName(serviceName),
      concurrency!!,
      factory!!,
      withTimeout,
      withRetry,
  )
}
