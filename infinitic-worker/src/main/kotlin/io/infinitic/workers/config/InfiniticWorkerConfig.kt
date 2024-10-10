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

import io.infinitic.config.loadFromYamlFile
import io.infinitic.config.loadFromYamlResource
import io.infinitic.config.loadFromYamlString
import io.infinitic.events.config.EventListenerConfig
import io.infinitic.storage.config.StorageConfig
import io.infinitic.transport.config.TransportConfig

@Suppress("unused")
data class InfiniticWorkerConfig(
  /** Worker name */
  override val name: String? = null,

  /** Transport configuration */
  override val transport: TransportConfig,

  /** Default storage */
  override val storage: StorageConfig? = null,

  /** Logs configuration */
  override val logs: LogsConfig = LogsConfig(),

  /** Workflows configuration */
  override val workflows: List<WorkflowConfig> = listOf(),

  /** Services configuration */
  override val services: List<ServiceConfig> = listOf(),

  /** Default event listener configuration */
  override val eventListener: EventListenerConfig? = null,

  ) : InfiniticWorkerConfigInterface {

  init {
    workflows.forEach { workflowConfig ->
      workflowConfig.stateEngine?.let {
        it.setStorage(storage)
          ?: throw IllegalArgumentException("Storage undefined for Workflow State Engine of '${workflowConfig.name}")
      }
      workflowConfig.tagEngine?.let {
        it.setStorage(storage)
          ?: throw IllegalArgumentException("Storage undefined for Workflow Tag Engine of '${workflowConfig.name}")
      }
    }
    services.forEach { serviceConfig ->
      serviceConfig.tagEngine?.let {
        it.setStorage(storage)
          ?: throw IllegalArgumentException("Storage undefined for Service Tag Engine of '${serviceConfig.name}")
      }
    }
  }

  companion object {
    /**
     * Create InfiniticWorkerConfig from files in file system
     */
    @JvmStatic
    fun fromYamlFile(vararg files: String): InfiniticWorkerConfig =
        loadFromYamlFile(*files)

    /**
     * Create InfiniticWorkerConfig from files in resources directory
     */
    @JvmStatic
    fun fromYamlResource(vararg resources: String): InfiniticWorkerConfig =
        loadFromYamlResource(*resources)

    /**
     * Create InfiniticWorkerConfig from yaml strings
     */
    @JvmStatic
    fun fromYamlString(vararg yamls: String): InfiniticWorkerConfig =
        loadFromYamlString(*yamls)
  }
}

internal interface WithMutableStorage {
  var storage: StorageConfig?
}

private fun WithMutableStorage.setStorage(storage: StorageConfig?): StorageConfig? {
  this.storage = this.storage ?: storage

  return this.storage
}
