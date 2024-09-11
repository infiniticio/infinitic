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
@file:Suppress("unused")

package io.infinitic.workers

import io.infinitic.storage.config.StorageConfig
import io.infinitic.transport.config.TransportConfig
import io.infinitic.workers.config.EventListenerConfig
import io.infinitic.workers.config.InfiniticWorkerConfig
import io.infinitic.workers.config.LogsConfig
import io.infinitic.workers.config.ServiceConfig
import io.infinitic.workers.config.ServiceExecutorConfig
import io.infinitic.workers.config.ServiceTagEngineConfig
import io.infinitic.workers.config.WorkflowConfig
import io.infinitic.workers.config.WorkflowExecutorConfig
import io.infinitic.workers.config.WorkflowStateEngineConfig
import io.infinitic.workers.config.WorkflowTagEngineConfig

/**
 * InfiniticWorker builder
 */
class InfiniticWorkerBuilder {
  private var name: String? = null
  private var transport: TransportConfig? = null
  private var storage: StorageConfig? = null
  private var logs: LogsConfig = LogsConfig()
  private var workflows: MutableList<WorkflowConfig> = mutableListOf()
  private var services: MutableList<ServiceConfig> = mutableListOf()
  private var eventListener: EventListenerConfig? = null

  private fun getOrCreateServiceConfig(serviceName: String) =
      services.firstOrNull { it.name == serviceName }
        ?: ServiceConfig(serviceName).also { services.add(it) }

  private fun getOrCreateWorkflowConfig(workflowName: String) =
      workflows.firstOrNull { it.name == workflowName }
        ?: WorkflowConfig(workflowName).also { workflows.add(it) }

  fun setName(name: String) =
      apply { this.name = name }

  fun setTransport(transport: TransportConfig) =
      apply { this.transport = transport }

  fun setTransport(transport: TransportConfig.TransportConfigBuilder) =
      setTransport(transport.build())

  fun setStorage(storage: StorageConfig) =
      apply { this.storage = storage }

  fun setStorage(storage: StorageConfig.StorageConfigBuilder) =
      setStorage(storage.build())

  fun setLogs(logs: LogsConfig) =
      apply { this.logs = logs }

  fun setLogs(logs: LogsConfig.LogConfigBuilder) =
      setLogs(logs.build())

  fun addServiceExecutor(config: ServiceExecutorConfig) =
      apply { getOrCreateServiceConfig(config.serviceName).executor = config }

  fun addServiceExecutor(config: ServiceExecutorConfig.ServiceExecutorConfigBuilder) =
      addServiceExecutor(config.build())

  fun addServiceTagEngine(config: ServiceTagEngineConfig) =
      apply { getOrCreateServiceConfig(config.serviceName).tagEngine = config }

  fun addServiceTagEngine(config: ServiceTagEngineConfig.ServiceTagEngineConfigBuilder) =
      addServiceTagEngine(config.build())

  fun addWorkflowExecutor(config: WorkflowExecutorConfig) =
      apply { getOrCreateWorkflowConfig(config.workflowName).executor = config }

  fun addWorkflowExecutor(config: WorkflowExecutorConfig.WorkflowExecutorConfigBuilder) =
      addWorkflowExecutor(config.build())

  fun addWorkflowStateEngine(config: WorkflowStateEngineConfig) =
      apply { getOrCreateWorkflowConfig(config.workflowName).stateEngine = config }

  fun addWorkflowStateEngine(config: WorkflowStateEngineConfig.WorkflowStateEngineConfigBuilder) =
      addWorkflowStateEngine(config.build())

  fun addWorkflowTagEngine(config: WorkflowTagEngineConfig) =
      apply { getOrCreateWorkflowConfig(config.workflowName).tagEngine = config }

  fun addWorkflowTagEngine(config: WorkflowTagEngineConfig.WorkflowTagEngineConfigBuilder) =
      addWorkflowTagEngine(config.build())

  fun setEventListener(eventListener: EventListenerConfig?) =
      apply { this.eventListener = eventListener }

  fun setEventListener(eventListener: EventListenerConfig.EventListenerConfigBuilder) =
      setEventListener(eventListener.build())

  fun build(): InfiniticWorker {
    require(transport != null) { "transport must not be null" }

    val config = InfiniticWorkerConfig(
        name,
        transport!!,
        storage,
        logs,
        workflows,
        services,
        eventListener,
    )

    return InfiniticWorker(config)
  }
}
