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

import io.infinitic.common.workers.config.RetryPolicy
import io.infinitic.events.config.EventListenerConfig
import io.infinitic.events.config.EventLoggerConfig
import io.infinitic.tasks.WithTimeout
import io.infinitic.tasks.tag.config.ServiceTagEngineConfig

@Suppress("unused")
data class ServiceConfigDefault(
  val concurrency: Int? = null,
  val timeoutInSeconds: Double? = null,
  val retry: RetryPolicy? = null,
  val tagEngine: ServiceTagEngineConfig? = null,
  val eventListener: EventListenerConfig? = null,
  val eventLogger: EventLoggerConfig? = null
) {
  init {
    concurrency?.let {
      require(it >= 0) { error("'${::concurrency.name}' must be positive") }
    }

    timeoutInSeconds?.let {
      require(it > 0) { error("'${::timeoutInSeconds.name}' must be strictly positive") }
    }
  }

  val withTimeout: WithTimeout? = when (timeoutInSeconds) {
    null -> null
    else -> WithTimeout { timeoutInSeconds }
  }

  companion object {
    @JvmStatic
    fun builder() = ServiceConfigDefaultBuilder()
  }

  /**
   * ServiceConfigDefault builder (Useful for Java user)
   */
  class ServiceConfigDefaultBuilder {
    private val default = ServiceConfigDefault()
    private var concurrency = default.concurrency
    private var timeoutInSeconds = default.timeoutInSeconds
    private var retry = default.retry
    private var tagEngine = default.tagEngine
    private var eventListener = default.eventListener
    private var eventLogger = default.eventLogger

    fun concurrency(concurrency: Int) =
        apply { this.concurrency = concurrency }

    fun timeoutInSeconds(timeoutInSeconds: Double) =
        apply { this.timeoutInSeconds = timeoutInSeconds }

    fun retry(retry: RetryPolicy) =
        apply { this.retry = retry }

    fun tagEngine(tagEngine: ServiceTagEngineConfig) =
        apply { this.tagEngine = tagEngine }

    fun eventListener(eventListener: EventListenerConfig) =
        apply { this.eventListener = eventListener }

    fun eventLogger(logger: EventLoggerConfig) =
        apply { this.eventLogger = logger }

    fun build() = ServiceConfigDefault(
        concurrency,
        timeoutInSeconds,
        retry,
        tagEngine,
        eventListener,
        eventLogger,
    )
  }

  private fun error(msg: String) = "default service: $msg"
}
