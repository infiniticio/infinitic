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
import io.infinitic.tasks.tag.config.TaskTag
import io.infinitic.workers.register.InfiniticRegisterInterface

data class Service(
  val name: String,
  val `class`: String? = null,
  var concurrency: Int? = null,
  var timeoutInSeconds: Double? = null,
  var retry: RetryPolicy? = null,
  var tagEngine: TaskTag? = InfiniticRegisterInterface.DEFAULT_TASK_TAG
) {
  fun getInstance(): Any = `class`!!.getInstance(
      classNotFound = error("Class '${`class`}' unknown"),
      errorClass = error("Can not access class '${`class`}'"),
      noEmptyConstructor = error("Class '${::`class`.name}' must have an empty constructor"),
      constructorError = error("Can not access class '$`class`' constructor"),
      instanceError = error("Error during instantiation of class '$`class`'"),
  ).getOrThrow()

  init {
    require(name.isNotEmpty()) { "name can not be empty" }

    when (`class`) {
      null -> {
        require(tagEngine != null) { error("class and taskTag undefined") }
      }

      else -> {
        require(`class`.isNotEmpty()) { error("class empty") }

        val instance = getInstance()

        require(instance::class.java.isImplementationOf(name)) {
          error("Class '${instance::class.java.name}' is not an implementation of this service - check your configuration")
        }

        if (concurrency != null) {
          require(concurrency!! >= 0) { error("'${::concurrency.name}' must be a positive integer") }
        }

        if (timeoutInSeconds != null) {
          require(timeoutInSeconds!! > 0) {
            error("'${::timeoutInSeconds.name}' must be positive")
          }
        }
      }
    }
  }

  private fun error(txt: String) = "Service $name: $txt"
}
