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
import io.infinitic.tasks.WithRetry
import io.infinitic.tasks.WithTimeout
import io.infinitic.tasks.tag.config.ServiceTagEngineConfig


@Suppress("unused")
data class ServiceConfig(
  val name: String,
  val `class`: String? = null,
  var concurrency: Int? = null,
  var timeoutSeconds: Double? = UNDEFINED_TIMEOUT,
  var retry: RetryPolicy? = UNDEFINED_RETRY,
  var tagEngine: ServiceTagEngineConfig? = DEFAULT_SERVICE_TAG_ENGINE,
) {
  fun getInstance(): Any = `class`!!.getInstance().getOrThrow()

  val withTimeout: WithTimeout? = when (timeoutSeconds) {
    null -> null
    UNDEFINED_TIMEOUT -> UNDEFINED_WITH_TIMEOUT
    else -> WithTimeout { timeoutSeconds }
  }

  val withRetry: WithRetry? = when (retry) {
    null -> null
    UNDEFINED_RETRY -> UNDEFINED_WITH_RETRY
    else -> retry
  }

  init {
    require(name.isNotEmpty()) { "'${::name.name}' can not be empty" }

    `class`?.let { klass ->
      require(klass.isNotEmpty()) { error("'class' can not be empty") }

      val instance = getInstance()

      require(instance::class.java.isImplementationOf(name)) {
        error("Class '${instance::class.java.name}' is not an implementation of this service - check your configuration")
      }

      concurrency?.let {
        require(it >= 0) { error("'${::concurrency.name}' must be an integer >= 0") }
      }

      timeoutSeconds?.let { timeout ->
        require(timeout > 0 || timeout == UNDEFINED_TIMEOUT) { error("'${::timeoutSeconds.name}' must be an integer > 0") }
      }
    }
  }

  companion object {
    @JvmStatic
    fun builder() = ServiceConfigBuilder()
  }

  /**
   * ServiceConfig builder (Useful for Java user)
   */
  class ServiceConfigBuilder {
    private val default = ServiceConfig(UNSET)
    private var name = default.name
    private var `class` = default.`class`
    private var concurrency = default.concurrency
    private var timeoutInSeconds = default.timeoutSeconds
    private var retry = default.retry
    private var tagEngine = default.tagEngine

    fun setName(name: String) =
        apply { this.name = name }

    fun setClass(`class`: String) =
        apply { this.`class` = `class` }

    fun setConcurrency(concurrency: Int) =
        apply { this.concurrency = concurrency }

    fun setTimeoutInSeconds(timeoutInSeconds: Double) =
        apply { this.timeoutInSeconds = timeoutInSeconds }

    fun setRetry(retry: RetryPolicy) =
        apply { this.retry = retry }

    fun setTagEngine(tagEngine: ServiceTagEngineConfig) =
        apply { this.tagEngine = tagEngine }

    fun build() = ServiceConfig(
        name.noUnset,
        `class`,
        concurrency,
        timeoutInSeconds,
        retry,
        tagEngine,
    )
  }

  private fun error(txt: String) = "Service $name: $txt"
}

private const val UNSET = "INFINITIC_UNSET_STRING"
private val String.noUnset: String
  get() = when (this) {
    UNSET -> ""
    else -> this
  }
