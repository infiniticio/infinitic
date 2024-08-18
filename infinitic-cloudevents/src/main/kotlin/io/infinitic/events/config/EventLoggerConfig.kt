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
package io.infinitic.events.config

import io.infinitic.logs.LogLevel

@Suppress("unused")
data class EventLoggerConfig(
  val concurrency: Int? = null,
  val logLevel: LogLevel? = null,
  val loggerName: String? = null,
  val beautify: Boolean? = null,
  val subscriptionName: String? = null,
) {
  var isDefined = true

  init {
    concurrency?.let {
      require(it > 0) {
        error("'${::concurrency.name}' must be an integer > 0")
      }
    }

    subscriptionName?.let {
      require(it.isNotEmpty()) { error("'${::subscriptionName.name}' must not be empty") }
    }

    loggerName?.let {
      require(it.isNotEmpty()) { error("'${::loggerName.name}' must not be empty") }
    }
  }


  companion object {
    @JvmStatic
    fun builder() = EventLoggerConfigBuilder()
  }

  /**
   * LogsConfigBuilder builder (Useful for Java user)
   */
  class EventLoggerConfigBuilder {
    private val default = EventLoggerConfig()
    private var logLevel = default.logLevel
    private var loggerName = default.loggerName
    private var beautify = default.beautify
    private var concurrency = default.concurrency
    private var subscriptionName = default.subscriptionName

    fun logLevel(logLevel: LogLevel) =
        apply { this.logLevel = logLevel }

    fun loggerName(loggerName: String) =
        apply { this.loggerName = loggerName }

    fun beautify(beautify: Boolean) =
        apply { this.beautify = beautify }

    fun concurrency(concurrency: Int) =
        apply { this.concurrency = concurrency }

    fun subscriptionName(subscriptionName: String) =
        apply { this.subscriptionName = subscriptionName }

    fun build() = EventLoggerConfig(
        concurrency = concurrency,
        logLevel = logLevel,
        loggerName = loggerName,
        beautify = beautify,
        subscriptionName = subscriptionName,
    )
  }
}
