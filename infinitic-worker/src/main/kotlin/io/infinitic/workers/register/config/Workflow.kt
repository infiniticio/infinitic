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
import io.infinitic.workers.register.InfiniticRegister
import io.infinitic.workflows.Workflow
import io.infinitic.workflows.WorkflowCheckMode
import io.infinitic.workflows.engine.config.WorkflowEngine
import io.infinitic.workflows.tag.config.WorkflowTag
import io.infinitic.workflows.Workflow as WorkflowBase

data class Workflow(
  val name: String,
  val `class`: String? = null,
  val classes: List<String>? = null,
  var concurrency: Int? = null,
  var timeoutInSeconds: Double? = null,
  var retry: RetryPolicy? = null,
  var checkMode: WorkflowCheckMode? = null,
  var tagEngine: WorkflowTag? = InfiniticRegister.DEFAULT_WORKFLOW_TAG,
  var workflowEngine: WorkflowEngine? = InfiniticRegister.DEFAULT_WORKFLOW_ENGINE
) {
  val allClasses = mutableListOf<Class<out WorkflowBase>>()

  init {
    require(name.isNotEmpty()) { "name can not be empty" }

    when {
      (`class` == null) && (classes == null) -> {
        require(tagEngine != null || workflowEngine != null) {
          "Workflow $name: '${::`class`.name}', '${::classes.name}', '${::tagEngine.name}' and '${::workflowEngine.name}' can not be all null"
        }
      }

      else -> {
        if (`class` != null) {
          require(`class`.isNotEmpty()) { "Workflow $name: '${::`class`.name}' empty" }
        }
        classes?.forEachIndexed { index, s: String ->
          require(s.isNotEmpty()) { "Workflow $name: '${::classes.name}[$index]' empty" }
        }

        `class`?.also { allClasses.add(getWorkflowClass(it)) }
        classes?.forEach { allClasses.add(getWorkflowClass(it)) }

        if (concurrency != null) {
          require(concurrency!! >= 0) { "Workflow $name: '${::concurrency.name}' must be a positive integer" }
        }

        if (timeoutInSeconds != null) {
          require(timeoutInSeconds!! > 0) { "Workflow $name: '${::timeoutInSeconds.name}' must be a positive integer" }
        }
      }
    }
  }

  private fun getWorkflowClass(className: String): Class<out Workflow> {
    val klass = className.getClass(
        classNotFound = "Workflow $name: Class '$className' unknown",
        errorClass = "Workflow $name: Unable to get class by name '$className'",
    )

    val constructor = klass.getEmptyConstructor(
        noEmptyConstructor = "Workflow $name: Class '$className' must have an empty constructor",
        constructorError = "Workflow $name: Can not access constructor of class '$className'",
    )

    val instance = constructor.getInstance(
        instanceError = "Workflow $name: Error when instantiating class '$className'",
    )

    require(klass.isImplementationOf(name)) {
      "Class '$klass' is not an implementation of workflow '$name' - check your configuration"
    }

    require(instance is WorkflowBase) {
      "Workflow $name: Class '$className' must extend '${WorkflowBase::class.java.name}'"
    }

    @Suppress("UNCHECKED_CAST") return klass as Class<out WorkflowBase>
  }
}
