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
