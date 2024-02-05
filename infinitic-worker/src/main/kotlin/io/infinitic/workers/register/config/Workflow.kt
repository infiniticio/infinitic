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
import io.infinitic.workflows.Workflow
import io.infinitic.workflows.WorkflowCheckMode
import io.infinitic.workflows.engine.config.WorkflowStateEngine
import io.infinitic.workflows.tag.config.WorkflowTagEngine
import io.infinitic.workflows.Workflow as WorkflowBase

data class Workflow(
  val name: String,
  val `class`: String? = null,
  val classes: List<String>? = null,
  var concurrency: Int? = null,
  var timeoutInSeconds: Double? = null,
  var retry: RetryPolicy? = null,
  var checkMode: WorkflowCheckMode? = null,
  var tagEngine: WorkflowTagEngine? = DEFAULT_TAG_ENGINE,
  var workflowEngine: WorkflowStateEngine? = DEFAULT_STATE_ENGINE,
  var eventListener: EventListener? = null,
) {
  val allClasses = mutableListOf<Class<out WorkflowBase>>()

  init {
    require(name.isNotEmpty()) { "name can not be empty" }

    when {
      (`class` == null) && (classes == null) -> require(tagEngine != null || workflowEngine != null || eventListener != null) {
        error("'${::`class`.name}', '${::classes.name}', '${::tagEngine.name}', '${::workflowEngine.name}' and '${::eventListener.name}' can not be all null")
      }

      else -> {
        if (`class` != null) {
          require(`class`.isNotEmpty()) { error("'${::`class`.name}' can not be empty") }
        }
        classes?.forEachIndexed { index, s: String ->
          require(s.isNotEmpty()) { error("'${::classes.name}[$index]' can not be empty") }
        }

        `class`?.also { allClasses.add(getWorkflowClass(it)) }
        classes?.forEach { allClasses.add(getWorkflowClass(it)) }

        if (concurrency != null) {
          require(concurrency!! >= 0) { error("'${::concurrency.name}' must be an integer >= 0") }
        }

        if (timeoutInSeconds != null) {
          require(timeoutInSeconds!! > 0) { error("'${::timeoutInSeconds.name}' must be an integer > 0") }
        }
      }
    }
  }

  private fun getWorkflowClass(className: String): Class<out Workflow> {
    val instance = className.getInstance().getOrThrow()

    val klass = instance::class.java

    require(klass.isImplementationOf(name)) {
      error("Class '${klass.name}' is not an implementation of this workflow - check your configuration")
    }

    require(instance is WorkflowBase) {
      error("Class '${klass.name}' must extend '${WorkflowBase::class.java.name}'")
    }

    @Suppress("UNCHECKED_CAST") return klass as Class<out WorkflowBase>
  }

  private fun error(txt: String) = "Workflow $name: $txt"

  companion object {
    val DEFAULT_STATE_ENGINE = WorkflowStateEngine().apply { isDefault = true }
    val DEFAULT_TAG_ENGINE = WorkflowTagEngine().apply { isDefault = true }
  }
}
