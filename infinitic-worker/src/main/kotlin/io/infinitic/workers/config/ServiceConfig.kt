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

@Suppress("unused")
data class ServiceConfig(
  /**
   * The unique name of the service.
   */
  val name: String,
  /**
   * The executor configuration for the service.
   * If not provided, it will not start an executor.
   */
  var executor: ServiceExecutorConfig? = null,
  /**
   * The tag engine configuration for the service.
   * If not provided, it will not start a tag engine.
   */
  var tagEngine: ServiceTagEngineConfig? = null,
) {
  init {
    require(name.isNotBlank()) { "'${::name.name}' can not be blank" }

    executor?.let {
      if (it is LoadedServiceExecutorConfig) it.setServiceName(name)
      val instance = it.factory()
      require(instance::class.java.isImplementationOf(name)) {
        error("Class '${instance::class.java.name}' must be an implementation of Service '$name', but is not.")
      }
    }

    tagEngine?.setServiceName(name)
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

internal interface WithMutableServiceName {
  var serviceName: String
}

private fun WithMutableServiceName.setServiceName(name: String) {
  if (serviceName == name) return
  if (serviceName.isNotBlank()) {
    throw IllegalStateException("${::serviceName.name} is already set to '$serviceName'")
  }
  serviceName = name
}
