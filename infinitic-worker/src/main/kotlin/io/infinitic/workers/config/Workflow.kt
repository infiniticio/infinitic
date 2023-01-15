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
package io.infinitic.workers.config

import io.infinitic.common.workers.config.RetryPolicy
import io.infinitic.workers.register.WorkerRegister
import io.infinitic.workflows.Workflow as WorkflowBase
import io.infinitic.workflows.Workflow
import io.infinitic.workflows.WorkflowCheckMode
import io.infinitic.workflows.engine.config.WorkflowEngine
import io.infinitic.workflows.tag.config.WorkflowTag

data class Workflow(
    val name: String,
    val `class`: String? = null,
    val classes: List<String>? = null,
    var concurrency: Int? = null,
    var timeoutInSeconds: Double? = null,
    var retry: RetryPolicy? = null,
    var checkMode: WorkflowCheckMode? = null,
    var tagEngine: WorkflowTag? = WorkerRegister.DEFAULT_WORKFLOW_TAG,
    var workflowEngine: WorkflowEngine? = WorkerRegister.DEFAULT_WORKFLOW_ENGINE
) {
  val allClasses = mutableListOf<Class<out WorkflowBase>>()

  init {
    require(name.isNotEmpty()) { "name can not be empty" }

    when {
      (`class` == null) && (classes == null) -> {
        require(tagEngine != null || workflowEngine != null) {
          "class, classes, workflowTag and workflowEngine are null for workflow $name"
        }
      }
      else -> {
        if (`class` != null) {
          require(`class`.isNotEmpty()) { "class empty for workflow $name" }
        }
        classes?.forEachIndexed { index, s ->
          require(s.isNotEmpty()) { "classes[$index] empty for workflow $name" }
        }

        `class`?.also { allClasses.add(getWorkflowClass(it)) }
        classes?.forEach { allClasses.add(getWorkflowClass(it)) }

        if (concurrency != null) {
          require(concurrency!! >= 0) { "concurrency must be positive (workflow $name)" }
        }

        if (timeoutInSeconds != null) {
          require(timeoutInSeconds!! > 0) { "timeoutSeconds must be positive (workflow $name)" }
        }
      }
    }
  }

  private fun getWorkflowClass(className: String): Class<out Workflow> {
    val klass =
        try {
          Class.forName(className)
        } catch (e: ClassNotFoundException) {
          throw IllegalArgumentException("class \"$className\" unknown for workflow $name")
        } catch (e: Exception) {
          throw IllegalArgumentException(
              "Error when trying to get class of name \"$className\" for workflow $name", e)
        }

    val constructor =
        try {
          klass.getDeclaredConstructor()
        } catch (e: NoSuchMethodException) {
          throw IllegalArgumentException(
              "class \"$className\" must have an empty constructor to be used as workflow $name")
        } catch (e: Exception) {
          throw IllegalArgumentException(
              "Error when trying to get constructor of class \"$className\" for workflow $name", e)
        }

    val instance =
        try {
          constructor.newInstance()
        } catch (e: Exception) {
          throw IllegalArgumentException(
              "Error when trying to instantiate class \"$className\" for workflow $name", e)
        }

    require(instance is WorkflowBase) {
      "class \"$className\" must extend ${WorkflowBase::class.java.name} to be used as workflow $name"
    }

    @Suppress("UNCHECKED_CAST") return klass as Class<out WorkflowBase>
  }
}
