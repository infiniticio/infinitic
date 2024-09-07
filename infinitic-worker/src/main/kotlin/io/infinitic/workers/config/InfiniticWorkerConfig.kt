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

import io.infinitic.common.utils.merge
import io.infinitic.config.loadFromYamlFile
import io.infinitic.config.loadFromYamlResource
import io.infinitic.config.loadFromYamlString
import io.infinitic.pulsar.config.PulsarConfig
import io.infinitic.storage.config.StorageConfig
import io.infinitic.transport.config.Transport
import io.infinitic.workers.registry.RegistryConfigInterface

@Suppress("unused")
data class InfiniticWorkerConfig(
  /** Worker name */
  val name: String? = null,

  /** Worker name */
  val shutdownGracePeriodSeconds: Double = 30.0,

  /** Transport configuration */
  val transport: Transport = Transport.pulsar,

  /** Pulsar configuration */
  val pulsar: PulsarConfig? = null,

  /** Default storage */
  val storage: StorageConfig? = null,

  /** Logs configuration */
  val logs: LogsConfig = LogsConfig(),

  /** Workflows configuration */
  override val workflows: List<WorkflowConfig> = listOf(),

  /** Services configuration */
  override val services: List<ServiceConfig> = listOf(),

  /** Default event listener configuration */
  override val eventListener: EventListenerConfig? = null,

  ) : RegistryConfigInterface {

  init {
    workflows.forEach { workflowConfig ->
      workflowConfig.stateEngine?.let {
        it.storage = it.storage.merge(storage)
          ?: throw IllegalArgumentException("Storage undefined for Workflow State Engine of '${workflowConfig.name}")
      }
      workflowConfig.tagEngine?.let {
        it.storage = it.storage.merge(storage)
          ?: throw IllegalArgumentException("Storage undefined for Workflow Tag Engine of '${workflowConfig.name}")
      }
    }
    services.forEach { serviceConfig ->
      serviceConfig.tagEngine?.let {
        it.storage = it.storage.merge(storage)
          ?: throw IllegalArgumentException("Storage undefined for Service Tag Engine of '${serviceConfig.name}")
      }
    }
  }

  companion object {
    @JvmStatic
    fun builder() = InfiniticWorkerConfigBuilder()

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

  /**
   * InfiniticWorkerConfig builder
   */
  class InfiniticWorkerConfigBuilder() {
    private val default = InfiniticWorkerConfig()
    private var name = default.name
    private var shutdownGracePeriodSeconds = default.shutdownGracePeriodSeconds
    private var transport = default.transport
    private var pulsar = default.pulsar
    private var storage = default.storage
    private var logs = default.logs
    private var workflows = default.workflows.toMutableList()
    private var services = default.services.toMutableList()
    private var eventListener = default.eventListener

    private fun getOrCreateServiceConfig(serviceName: String) =
        services.firstOrNull { it.name == serviceName }
          ?: ServiceConfig(serviceName).also { services.add(it) }

    private fun getOrCreateWorkflowConfig(workflowName: String) =
        workflows.firstOrNull { it.name == workflowName }
          ?: WorkflowConfig(workflowName).also { workflows.add(it) }

    fun setName(name: String) =
        apply { this.name = name }

    fun setShutdownGracePeriodSeconds(shutdownGracePeriodSeconds: Double) =
        apply { this.shutdownGracePeriodSeconds = shutdownGracePeriodSeconds }

    fun setTransport(transport: Transport) =
        apply { this.transport = transport }

    fun setPulsar(pulsar: PulsarConfig?) =
        apply { this.pulsar = pulsar }

    fun setStorage(storage: StorageConfig?) =
        apply { this.storage = storage }

    fun setLogs(logs: LogsConfig) =
        apply { this.logs = logs }

    fun addServiceExecutor(serviceName: String, executorConfig: ServiceExecutorConfig) =
        apply { getOrCreateServiceConfig(serviceName).executor = executorConfig }

    fun addServiceTagEngine(serviceName: String, tagEngineConfig: ServiceTagEngineConfig) =
        apply { getOrCreateServiceConfig(serviceName).tagEngine = tagEngineConfig }

    fun addWorkflowExecutor(
      workflowName: String,
      workflowExecutorConfig: WorkflowExecutorConfig
    ) = apply { getOrCreateWorkflowConfig(workflowName).executor = workflowExecutorConfig }


    fun addWorkflowStateEngine(
      workflowName: String,
      stateEngineConfig: WorkflowStateEngineConfig
    ) = apply { getOrCreateWorkflowConfig(workflowName).stateEngine = stateEngineConfig }


    fun addWorkflowTagEngine(
      workflowName: String,
      tagEngineConfig: WorkflowTagEngineConfig
    ) = apply { getOrCreateWorkflowConfig(workflowName).tagEngine = tagEngineConfig }


    fun setEventListener(eventListener: EventListenerConfig?) =
        apply { this.eventListener = eventListener }


    fun build() = InfiniticWorkerConfig(
        name,
        shutdownGracePeriodSeconds,
        transport,
        pulsar,
        storage,
        logs,
        workflows,
        services,
    )
  }
}
