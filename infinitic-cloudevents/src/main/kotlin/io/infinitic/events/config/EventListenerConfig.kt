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

import io.infinitic.cloudEvents.CloudEventListener
import io.infinitic.cloudEvents.SelectionConfig
import io.infinitic.common.utils.getInstance

@Suppress("unused")
data class EventListenerConfig(
  val `class`: String,
  val concurrency: Int? = null,
  val subscriptionName: String? = null,
  val services: SelectionConfig = SelectionConfig(),
  val workflows: SelectionConfig = SelectionConfig()
) {
  var isDefined = true

  val instance: CloudEventListener

  init {
    with(`class`) {
      require(isNotEmpty()) { error("'class' must not be empty") }
      val obj = `class`.getInstance().getOrThrow()
      require(obj is CloudEventListener) {
        error("Class '$`class`' must implement '${CloudEventListener::class.java.name}'")
      }
      instance = obj
    }

    concurrency?.let {
      require(it >= 0) {
        error("'${::concurrency.name}' must be an integer >= 0")
      }
    }

    subscriptionName?.let {
      require(it.isNotEmpty()) { error("'${::subscriptionName.name}' must not be empty") }
    }
  }

  companion object {
    @JvmStatic
    fun builder() = EventListenerConfigBuilder()
  }


  /**
   * EventListenerConfig builder (Useful for Java user)
   */
  class EventListenerConfigBuilder {
    private val default = EventListenerConfig(UNSET)
    private var `class` = default.`class`
    private var concurrency = default.concurrency
    private var subscriptionName = default.subscriptionName
    private var services = default.services
    private var workflows = default.workflows

    fun setClass(`class`: String) =
        apply { this.`class` = `class` }

    fun setConcurrency(concurrency: Int) =
        apply { this.concurrency = concurrency }

    fun setSubscriptionName(subscriptionName: String) =
        apply { this.subscriptionName = subscriptionName }

    fun setServices(services: SelectionConfig) =
        apply { this.services = services }

    fun setWorkflows(workflows: SelectionConfig) =
        apply { this.workflows = workflows }

    fun build() = EventListenerConfig(
        `class`.removeUnset(),
        concurrency,
        subscriptionName,
        services,
        workflows,
    )
  }

  private fun error(txt: String) = "eventListener: $txt"


}

private const val UNSET = "INFINITIC_UNSET_STRING"
private fun String.removeUnset(unset: String = UNSET): String = when (this) {
  unset -> ""
  else -> this
}
