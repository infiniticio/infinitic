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
import io.infinitic.common.utils.getInstance

data class EventListener(
  var `class`: String? = null,
  var concurrency: Int? = null,
  var subscriptionName: String? = null,
) {
  var isDefined = true

  val instance: CloudEventListener
    get() = `class`!!.getInstance().getOrThrow() as CloudEventListener

  init {
    `class`?.let {
      require(it.isNotEmpty()) { error("'class' must not be empty") }
      val instance = it.getInstance().getOrThrow()
      require(instance is CloudEventListener) {
        error("Class '$`class`' must implement '${CloudEventListener::class.java.name}'")
      }
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

  private fun error(txt: String) = "eventListener: $txt"
}
