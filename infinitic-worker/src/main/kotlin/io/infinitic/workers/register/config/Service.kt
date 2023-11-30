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

import io.infinitic.common.utils.getClass
import io.infinitic.common.utils.getEmptyConstructor
import io.infinitic.common.utils.getInstance
import io.infinitic.common.utils.isImplementationOf
import io.infinitic.common.workers.config.RetryPolicy
import io.infinitic.tasks.tag.config.TaskTag
import io.infinitic.workers.register.InfiniticRegister
import java.lang.reflect.Constructor

data class Service(
  val name: String,
  val `class`: String? = null,
  var concurrency: Int? = null,
  var timeoutInSeconds: Double? = null,
  var retry: RetryPolicy? = null,
  var tagEngine: TaskTag? = InfiniticRegister.DEFAULT_TASK_TAG
) {
  private lateinit var constructor: Constructor<out Any>

  fun getInstance(): Any = constructor.getInstance()

  init {
    require(name.isNotEmpty()) { "name can not be empty" }

    when (`class`) {
      null -> {
        require(tagEngine != null) { "Service $name: class and taskTag undefined" }
      }

      else -> {
        require(`class`.isNotEmpty()) { "Service $name: class empty" }

        val klass = `class`.getClass(
            classNotFound = "Service $name: Class '${`class`}' unknown",
            errorClass =
            "Service $name: Error when trying to get class of name '${::`class`.name}'",
        )

        constructor = klass.getEmptyConstructor(
            noEmptyConstructor =
            "Service $name: Class '${::`class`.name}' must have an empty constructor",
            constructorError =
            "Service $name: Can not access constructor of class '$`class`'",
        )

        constructor.getInstance(
            instanceError = "Service $name: Error during instantiation of class '$`class`'",
        )

        require(klass.isImplementationOf(name)) {
          "Class '$klass' is not an implementation of service '$name' - check your configuration"
        }

        if (concurrency != null) {
          require(concurrency!! >= 0) { "Service $name: '${::concurrency.name}' must be a positive integer" }
        }

        if (timeoutInSeconds != null) {
          require(timeoutInSeconds!! > 0) {
            "Service $name: '${::timeoutInSeconds.name}' must be positive"
          }
        }
      }
    }
  }
}
