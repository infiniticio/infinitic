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
package io.infinitic.common.workers.registry

import com.fasterxml.jackson.databind.ObjectMapper
import io.cloudevents.CloudEvent
import io.cloudevents.jackson.JsonFormat
import io.github.oshai.kotlinlogging.KotlinLogging
import io.infinitic.logs.LogLevel

data class RegisteredEventLogger(
  val concurrency: Int,
  val logLevel: LogLevel,
  val loggerName: String,
  val beautify: Boolean,
  val subscriptionName: String?
) {
  private val logger = KotlinLogging.logger(loggerName)
  private val reader = ObjectMapper().reader()
  private val writer = when (beautify) {
    true -> ObjectMapper().writerWithDefaultPrettyPrinter()
    false -> ObjectMapper().writer()
  }

  val log = { message: CloudEvent -> logString(message.toJsonString()) }

  private fun CloudEvent.toJsonString(): String {
    val jsonStr = String(JsonFormat().serialize(this))

    return writer.writeValueAsString(reader.readTree(jsonStr))
  }

  private val logString by lazy {
    when (logLevel) {
      LogLevel.TRACE -> { message: String -> logger.trace { message } }
      LogLevel.DEBUG -> { message: String -> logger.debug { message } }
      LogLevel.INFO -> { message: String -> logger.info { message } }
      LogLevel.WARN -> { message: String -> logger.warn { message } }
      LogLevel.ERROR -> { message: String -> logger.error { message } }
      LogLevel.OFF -> { _: String -> Unit }
    }
  }
}
