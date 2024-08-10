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
import io.infinitic.common.workflows.emptyWorkflowContext
import io.infinitic.events.config.EventListenerConfig
import io.infinitic.workflows.Workflow
import io.infinitic.workflows.WorkflowCheckMode
import io.infinitic.workflows.engine.config.WorkflowStateEngine
import io.infinitic.workflows.tag.config.WorkflowTagEngine
import io.infinitic.workflows.Workflow as WorkflowBase

/**
 * Represents a workflow entity with its properties and validation logic.
 *
 * @property name The name of the workflow.
 * @property class The class name for the workflow. Optional.
 * @property classes A list of class names for the workflow. Optional.
 * @property concurrency The concurrency value for the workflow. Optional.
 * @property timeoutInSeconds The timeout value in seconds for the workflow. Optional.
 * @property retry The retry policy for the workflow. Optional.
 * @property checkMode The workflow check mode. Optional.
 * @property tagEngine The tag engine for the workflow. Optional.
 * @property stateEngine The workflow state engine for the workflow. Optional.
 * @property eventListener The event listener for the workflow. Optional.
 * @property allClasses A mutable list of workflow base classes.
 *
 * @constructor Creates a Workflow instance.
 *
 * @throws IllegalArgumentException if the name is empty or all relevant properties are null.
 * @throws IllegalArgumentException if the class or any of the classes are empty.
 * @throws IllegalArgumentException if the concurrency is less than 0.
 * @throws IllegalArgumentException if the timeoutInSeconds is not greater than 0 or UNDEFINED_TIMEOUT.
 */
data class WorkflowConfig(
  val name: String,
  val `class`: String? = null,
  val classes: List<String>? = null,
  var concurrency: Int? = null,
  var timeoutInSeconds: Double? = UNDEFINED_TIMEOUT,
  var retry: RetryPolicy? = UNDEFINED_RETRY,
  var checkMode: WorkflowCheckMode? = null,
  var tagEngine: WorkflowTagEngine? = DEFAULT_WORKFLOW_TAG_ENGINE,
  var stateEngine: WorkflowStateEngine? = DEFAULT_WORKFLOW_STATE_ENGINE,
  var eventListener: EventListenerConfig? = UNDEFINED_EVENT_LISTENER,
) {
  val allClasses = mutableListOf<Class<out WorkflowBase>>()

  init {
    require(name.isNotEmpty()) { "name can not be empty" }

    when {
      (`class` == null) && (classes == null) -> require(tagEngine != null || stateEngine != null || eventListener != null) {
        error("'${::`class`.name}', '${::classes.name}', '${::tagEngine.name}', '${::stateEngine.name}' and '${::eventListener.name}' can not be all null")
      }

      else -> {
        `class`?.let {
          require(`class`.isNotEmpty()) { error("'${::`class`.name}' can not be empty") }
          allClasses.add(getWorkflowClass(it))
        }
        classes?.forEachIndexed { index, s: String ->
          require(s.isNotEmpty()) { error("'${::classes.name}[$index]' can not be empty") }
          allClasses.add(getWorkflowClass(s))
        }

        concurrency?.let {
          require(it >= 0) { error("'${::concurrency.name}' must be an integer >= 0") }
        }

        timeoutInSeconds?.let { timeout ->
          require(timeout > 0 || timeout == UNDEFINED_TIMEOUT) { error("'${::timeoutInSeconds.name}' must be an integer > 0") }
        }
      }
    }
  }

  private fun getWorkflowClass(className: String): Class<out Workflow> {
    // make sure to have a context to be able to create the workflow instance
    Workflow.setContext(emptyWorkflowContext)

    val instance = className.getInstance().getOrThrow()
    val klass = instance::class.java

    require(klass.isImplementationOf(name)) {
      error("Class '${klass.name}' is not an implementation of '$name' - check your configuration")
    }

    require(instance is WorkflowBase) {
      error("Class '${klass.name}' must extend '${WorkflowBase::class.java.name}'")
    }

    @Suppress("UNCHECKED_CAST") return klass as Class<out WorkflowBase>
  }

  private fun error(txt: String) = "Workflow $name: $txt"
}
