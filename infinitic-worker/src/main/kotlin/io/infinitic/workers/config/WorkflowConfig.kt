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
      val instances = it.factories.map { it() }
      instances.forEach { instance ->
        require(instance::class.java.isImplementationOf(name)) {
          error("Class '${instance::class.java.name}' must be an implementation of Workflow '$name'")
        }
      }
    }
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

