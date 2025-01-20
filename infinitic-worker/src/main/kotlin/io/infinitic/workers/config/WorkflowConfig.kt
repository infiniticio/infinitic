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

import io.infinitic.common.utils.isImplementationOf
import io.infinitic.config.loadFromYamlFile
import io.infinitic.config.loadFromYamlResource
import io.infinitic.config.loadFromYamlString

data class WorkflowConfig(
  val name: String,
  var executor: WorkflowExecutorConfig? = null,
  var tagEngine: WorkflowTagEngineConfig? = null,
  var stateEngine: WorkflowStateEngineConfig? = null,
) {
  init {
    require(name.isNotEmpty()) { "'${::name.name}' can not be empty" }

    executor?.let {
      if (it is LoadedWorkflowExecutorConfig) it.setWorkflowName(name)
      val instances = it.factories.map { it() }
      instances.forEach { instance ->
        require(instance::class.java.isImplementationOf(name)) {
          error("Class '${instance::class.java.name}' must be an implementation of Workflow '$name'")
        }
      }
    }
    tagEngine?.setWorkflowName(name)
    stateEngine?.setWorkflowName(name)
  }

  companion object {
    /**
     * Create WorkflowConfig from files in file system
     */
    @JvmStatic
    fun fromYamlFile(vararg files: String): WorkflowConfig =
        loadFromYamlFile(*files)

    /**
     * Create WorkflowConfig from files in resources directory
     */
    @JvmStatic
    fun fromYamlResource(vararg resources: String): WorkflowConfig =
        loadFromYamlResource(*resources)

    /**
     * Create WorkflowConfig from yaml strings
     */
    @JvmStatic
    fun fromYamlString(vararg yamls: String): WorkflowConfig =
        loadFromYamlString(*yamls)
  }
}

interface WithMutableWorkflowName {
  var workflowName: String
}

private fun WithMutableWorkflowName.setWorkflowName(name: String) {
  if (workflowName == name) return
  if (workflowName.isNotBlank()) {
    throw IllegalStateException("${::workflowName.name} is already set to '$workflowName'")
  }
  workflowName = name
}
