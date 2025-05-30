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

import io.infinitic.common.exceptions.thisShouldNotHappen
import io.infinitic.config.loadFromYamlFile
import io.infinitic.config.loadFromYamlResource
import io.infinitic.config.loadFromYamlString
import io.infinitic.storage.config.StorageConfig
import io.infinitic.tasks.tag.storage.BinaryTaskTagStorage

@Suppress("unused")
data class ServiceTagEngineConfig(
  override var serviceName: String = "",
  /**
   * The number of concurrent service tag executions.
   * If not provided, it will default to 1.
   */
  val concurrency: Int = 1,
  /**
   * Storage configuration for the service tag engine.
   * If not provided, it will use the default storage configuration.
   */
  override var storage: StorageConfig? = null,
) : WithMutableServiceName, WithMutableStorage {
  init {
    require(concurrency > 0) { "${::concurrency.name} must be > 0" }
  }

  val serviceTagStorage by lazy {
    (storage ?: thisShouldNotHappen()).let {
      BinaryTaskTagStorage(it.keyValue, it.keySet)
    }
  }

  companion object {
    @JvmStatic
    fun builder() = ServiceTagEngineConfigBuilder()

    /**
     * Create ServiceTagEngineConfig from files in the file system
     */
    @JvmStatic
    fun fromYamlFile(vararg files: String): ServiceTagEngineConfig =
        loadFromYamlFile(*files)

    /**
     * Create ServiceTagEngineConfig from files in the resources directory
     */
    @JvmStatic
    fun fromYamlResource(vararg resources: String): ServiceTagEngineConfig =
        loadFromYamlResource(*resources)

    /**
     * Create ServiceTagEngineConfig from YAML strings
     */
    @JvmStatic
    fun fromYamlString(vararg yamls: String): ServiceTagEngineConfig =
        loadFromYamlString(*yamls)

  }

  /**
   * ServiceTagEngineConfig builder
   */
  class ServiceTagEngineConfigBuilder {
    private val default = ServiceTagEngineConfig()
    private var serviceName = default.serviceName
    private var concurrency = default.concurrency
    private var storage = default.storage

    fun setServiceName(serviceName: String) =
        apply { this.serviceName = serviceName }

    fun setConcurrency(concurrency: Int) =
        apply { this.concurrency = concurrency }

    fun setStorage(storage: StorageConfig) =
        apply { this.storage = storage }

    fun setStorage(storage: StorageConfig.StorageConfigBuilder) =
        apply { this.storage = storage.build() }

    fun build(): ServiceTagEngineConfig {
      serviceName.checkServiceName()

      return ServiceTagEngineConfig(
          serviceName,
          concurrency,
          storage,
      )
    }
  }
}
