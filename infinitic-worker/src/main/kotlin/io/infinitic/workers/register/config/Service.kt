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
package io.infinitic.workers.register.config

import io.infinitic.common.utils.getInstance
import io.infinitic.common.utils.isImplementationOf
import io.infinitic.common.workers.config.RetryPolicy
import io.infinitic.events.config.EventListener
import io.infinitic.tasks.tag.config.ServiceTagEngine

/**
 * Represents a service.
 *
 * @property name The name of the service.
 * @property class The fully qualified class name of the service implementation.
 * @property concurrency The number of concurrent messages that can be handled by the service.
 * @property timeoutInSeconds The timeout value for the service in seconds.
 * @property retry The retry policy for the service.
 * @property tagEngine The tag engine for the service.
 * @property eventListener The event listener for the service.
 */
data class Service(
  val name: String,
  val `class`: String? = null,
  var concurrency: Int? = null,
  var timeoutInSeconds: Double? = UNDEFINED_TIMEOUT,
  var retry: RetryPolicy? = UNDEFINED_RETRY,
  var tagEngine: ServiceTagEngine? = DEFAULT_SERVICE_TAG,
  var eventListener: EventListener? = UNDEFINED_EVENT_LISTENER,
) {
  fun getInstance(): Any = `class`!!.getInstance().getOrThrow()

  init {
    require(name.isNotEmpty()) { "'${::name.name}' can not be empty" }

    `class`?.let { klass ->
      require(klass.isNotEmpty()) { error("'class' empty") }

      val instance = getInstance()

      require(instance::class.java.isImplementationOf(name)) {
        error("Class '${instance::class.java.name}' is not an implementation of this service - check your configuration")
      }

      concurrency?.let {
        require(it >= 0) { error("'${::concurrency.name}' must be an integer >= 0") }
      }

      timeoutInSeconds?.let  { timeout ->
        require(timeout > 0 || timeout == UNDEFINED_TIMEOUT ) { error("'${::timeoutInSeconds.name}' must be an integer > 0") }
      }
    }
  }

  private fun error(txt: String) = "Service $name: $txt"
}
