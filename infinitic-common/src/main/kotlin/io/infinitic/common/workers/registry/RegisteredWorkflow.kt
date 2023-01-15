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

import io.infinitic.common.exceptions.thisShouldNotHappen
import io.infinitic.common.workers.config.WorkflowVersion
import io.infinitic.common.workflows.data.workflows.WorkflowName
import io.infinitic.exceptions.workflows.UnknownWorkflowVersionException
import io.infinitic.tasks.WithRetry
import io.infinitic.tasks.WithTimeout
import io.infinitic.workflows.Workflow
import io.infinitic.workflows.WorkflowCheckMode

typealias WorkflowClassList = List<Class<out Workflow>>

data class RegisteredWorkflow(
    val workflowName: WorkflowName,
    val classes: WorkflowClassList,
    val concurrency: Int,
    val withTimeout: WithTimeout?,
    val withRetry: WithRetry?,
    val checkMode: WorkflowCheckMode?
) {
  init {
    require(classes.isNotEmpty()) { "List of classes must not be empty for workflow $workflowName" }

    // throw IllegalArgumentException if some classes have the same version
    with(classes.associateWith { WorkflowVersion.from(it) }) {
      values
          .groupingBy { it }
          .eachCount()
          .filter { it.value > 1 }
          .keys
          .forEach { version ->
            val list = filter { version == it.value }.keys.map { it.name }.joinToString()

            throw IllegalArgumentException(
                "$list have same version $version for workflow $workflowName")
          }
    }
  }

  private val classByVersion by lazy { classes.associateBy { WorkflowVersion.from(it) } }

  private val lastVersion by lazy { classByVersion.keys.maxOrNull() ?: thisShouldNotHappen() }
  fun getInstance(workflowVersion: WorkflowVersion?): Workflow =
      getClass(workflowVersion).getDeclaredConstructor().newInstance()

  private fun getClass(workflowVersion: WorkflowVersion?) =
      (workflowVersion ?: lastVersion).run {
        classByVersion[this] ?: throw UnknownWorkflowVersionException(workflowName, this)
      }
}
