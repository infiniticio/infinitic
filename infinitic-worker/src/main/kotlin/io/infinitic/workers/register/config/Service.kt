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
import io.infinitic.tasks.tag.config.TaskTag
import io.infinitic.workers.register.InfiniticRegisterInterface

data class Service(
  val name: String,
  val `class`: String? = null,
  var concurrency: Int? = null,
  var timeoutInSeconds: Double? = null,
  var retry: RetryPolicy? = null,
  var tagEngine: TaskTag? = InfiniticRegisterInterface.DEFAULT_SERVICE_TAG,
  var eventListener: EventListener? = null,
) {
  fun getInstance(): Any = `class`!!.getInstance().getOrThrow()

  init {
    require(name.isNotEmpty()) { "'${::name.name}' can not be empty" }

    when (`class`) {
      null -> {
        require(tagEngine != null || eventListener != null) {
          error("'Nothing defined for `name: $name`")
        }
      }

      else -> {
        require(`class`.isNotEmpty()) { error("'class' empty") }

        val instance = getInstance()

        require(instance::class.java.isImplementationOf(name)) {
          error("Class '${instance::class.java.name}' is not an implementation of this service - check your configuration")
        }

        if (concurrency != null) {
          require(concurrency!! >= 0) {
            error("'${::concurrency.name}' must be an integer >= 0")
          }
        }

        if (timeoutInSeconds != null) {
          require(timeoutInSeconds!! > 0) {
            error("'${::timeoutInSeconds.name}' must be an integer > 0")
          }
        }
      }
    }
  }

  private fun error(txt: String) = "Service $name: $txt"
}
