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
package io.infinitic.workers

import io.infinitic.pulsar.config.PulsarConfig
import io.infinitic.storage.config.StorageConfig
import io.infinitic.transport.config.Transport
import io.infinitic.workers.config.*

/**
 * InfiniticWorker builder
 */
class InfiniticWorkerBuilder() {
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

    fun setPulsar(pulsar: PulsarConfig.PulsarConfigBuilder) =
        setPulsar(pulsar.build())

    fun setStorage(storage: StorageConfig?) =
        apply { this.storage = storage }

    fun setStorage(storage: StorageConfig.StorageConfigBuilder) =
        setStorage(storage.build())

    fun setLogs(logs: LogsConfig) =
        apply { this.logs = logs }

    fun setLogs(logs: LogsConfig.LogConfigBuilder) =
        setLogs(logs.build())

    fun addServiceExecutor(serviceName: String, executorConfig: ServiceExecutorConfig) =
        apply { getOrCreateServiceConfig(serviceName).executor = executorConfig }

    fun addServiceExecutor(
        serviceName: String,
        executorConfig: ServiceExecutorConfig.ServiceExecutorConfigBuilder
    ) = addServiceExecutor(serviceName, executorConfig.build())

    fun addServiceTagEngine(serviceName: String, tagEngineConfig: ServiceTagEngineConfig) =
        apply { getOrCreateServiceConfig(serviceName).tagEngine = tagEngineConfig }

    fun addServiceTagEngine(
        serviceName: String,
        tagEngineConfig: ServiceTagEngineConfig.ServiceTagEngineConfigBuilder
    ) = addServiceTagEngine(serviceName, tagEngineConfig.build())

    fun addWorkflowExecutor(
        workflowName: String,
        workflowExecutorConfig: WorkflowExecutorConfig
    ) = apply { getOrCreateWorkflowConfig(workflowName).executor = workflowExecutorConfig }

    fun addWorkflowExecutor(
        workflowName: String,
        workflowExecutorConfig: WorkflowExecutorConfig.WorkflowExecutorConfigBuilder
    ) = addWorkflowExecutor(workflowName, workflowExecutorConfig.build())

    fun addWorkflowStateEngine(
        workflowName: String,
        stateEngineConfig: WorkflowStateEngineConfig
    ) = apply { getOrCreateWorkflowConfig(workflowName).stateEngine = stateEngineConfig }

    fun addWorkflowStateEngine(
        workflowName: String,
        stateEngineConfig: WorkflowStateEngineConfig.WorkflowStateEngineConfigBuilder
    ) = addWorkflowStateEngine(workflowName, stateEngineConfig.build())

    fun addWorkflowTagEngine(
        workflowName: String,
        tagEngineConfig: WorkflowTagEngineConfig
    ) = apply { getOrCreateWorkflowConfig(workflowName).tagEngine = tagEngineConfig }

    fun addWorkflowTagEngine(
        workflowName: String,
        tagEngineConfig: WorkflowTagEngineConfig.WorkflowTagEngineConfigBuilder
    ) = addWorkflowTagEngine(workflowName, tagEngineConfig.build())

    fun setEventListener(eventListener: EventListenerConfig?) =
        apply { this.eventListener = eventListener }

    fun setEventListener(eventListener: EventListenerConfig.EventListenerConfigBuilder) =
        setEventListener(eventListener.build())

    fun build() = InfiniticWorker.fromConfig(
        InfiniticWorkerConfig(
            name,
            shutdownGracePeriodSeconds,
            transport,
            pulsar,
            storage,
            logs,
            workflows,
            services,
        ),
    )
}
