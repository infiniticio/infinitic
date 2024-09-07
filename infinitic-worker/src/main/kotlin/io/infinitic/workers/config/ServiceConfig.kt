package io.infinitic.workers.config

import io.infinitic.common.utils.isImplementationOf
import io.infinitic.config.loadFromYamlFile
import io.infinitic.config.loadFromYamlResource
import io.infinitic.config.loadFromYamlString

@Suppress("unused")
data class ServiceConfig(
  val name: String,
  var executor: ServiceExecutorConfig? = null,
  var tagEngine: ServiceTagEngineConfig? = null,
) {
  init {
    require(name.isNotEmpty()) { "'${::name.name}' can not be empty" }

    executor?.let {
      val instance = it.factory()
      require(instance::class.java.isImplementationOf(name)) {
        error("Class '${instance::class.java.name}' must be an implementation of Service '$name', but is not.")
      }
    }
  }

  companion object {
    /**
     * Create ServiceConfig from files in file system
     */
    @JvmStatic
    fun fromYamlFile(vararg files: String): ServiceConfig =
        loadFromYamlFile(*files)

    /**
     * Create ServiceConfig from files in resources directory
     */
    @JvmStatic
    fun fromYamlResource(vararg resources: String): ServiceConfig =
        loadFromYamlResource(*resources)

    /**
     * Create ServiceExecutorConfig from yaml strings
     */
    @JvmStatic
    fun fromYamlString(vararg yamls: String): ServiceConfig =
        loadFromYamlString(*yamls)
  }
}
