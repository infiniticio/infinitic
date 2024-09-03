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

import io.infinitic.common.config.loadConfigFromFile
import io.infinitic.common.config.loadConfigFromResource
import io.infinitic.common.config.loadConfigFromYaml
import io.infinitic.events.config.EventListenerConfig
import io.infinitic.pulsar.config.PulsarConfig
import io.infinitic.storage.config.StorageConfig
import io.infinitic.transport.config.Transport
import io.infinitic.workers.register.config.LogsConfig
import io.infinitic.workers.register.config.ServiceConfig
import io.infinitic.workers.register.config.ServiceConfigDefault
import io.infinitic.workers.register.config.WorkflowConfig
import io.infinitic.workers.register.config.WorkflowConfigDefault

@Suppress("unused")
data class WorkerConfig(
  /** Worker name */
  override val name: String? = null,

  /** Worker name */
  override val shutdownGracePeriodSeconds: Double = 30.0,

  /** Transport configuration */
  override val transport: Transport = Transport.pulsar,

  /** Pulsar configuration */
  override val pulsar: PulsarConfig? = null,

  /** Default storage */
  override val storage: StorageConfig? = null,

  /** Logs configuration */
  override val logs: LogsConfig = LogsConfig(),

  /** Workflows configuration */
  override val workflows: List<WorkflowConfig> = listOf(),

  /** Services configuration */
  override val services: List<ServiceConfig> = listOf(),

  /** Default service configuration */
  override val serviceDefault: ServiceConfigDefault? = null,

  /** Default workflow configuration */
  override val workflowDefault: WorkflowConfigDefault? = null,

  /** Default event listener configuration */
  override val eventListener: EventListenerConfig? = null,

  ) : WorkerConfigInterface {

  init {
// check retry values
    serviceDefault?.retry?.check()
    services.forEach { it.retry?.check() }
    workflowDefault?.retry?.check()
    workflows.forEach { it.retry?.check() }
  }

  companion object {
    /** Create ClientConfig from files in file system */
    @JvmStatic
    fun fromFile(vararg files: String): WorkerConfig =
        loadConfigFromFile(*files)

    /** Create ClientConfig from files in resources directory */
    @JvmStatic
    fun fromResource(vararg resources: String): WorkerConfig =
        loadConfigFromResource(*resources)

    /** Create ClientConfig from yaml strings */
    @JvmStatic
    fun fromYaml(vararg yamls: String): WorkerConfig =
        loadConfigFromYaml(*yamls)

    @JvmStatic
    fun builder() = WorkerConfigBuilder()
  }

  /**
   * WorkerConfig builder (Useful for Java user)
   */
  class WorkerConfigBuilder {
    private val default = WorkerConfig()
    private var name = default.name
    private var shutdownGracePeriodInSeconds = default.shutdownGracePeriodSeconds
    private var transport = default.transport
    private var pulsar = default.pulsar
    private var storage = default.storage
    private var logs = default.logs
    private var workflows = default.workflows
    private var services = default.services
    private var serviceDefault = default.serviceDefault
    private var workflowDefault = default.workflowDefault
    private var eventListener = default.eventListener

    fun setName(name: String) =
        apply { this.name = name }

    fun setShutdownGracePeriodInSeconds(shutdownGracePeriodInSeconds: Double) =
        apply { this.shutdownGracePeriodInSeconds = shutdownGracePeriodInSeconds }

    fun setTransport(transport: Transport) =
        apply { this.transport = transport }

    fun setPulsar(pulsar: PulsarConfig?) =
        apply { this.pulsar = pulsar }

    fun setStorage(storage: StorageConfig?) =
        apply { this.storage = storage }

    fun setLogs(logs: LogsConfig) =
        apply { this.logs = logs }

    fun setWorkflows(workflows: List<WorkflowConfig>) =
        apply { this.workflows = workflows }

    fun setServices(services: List<ServiceConfig>) =
        apply { this.services = services }

    fun setServiceDefault(serviceDefault: ServiceConfigDefault?) =
        apply { this.serviceDefault = serviceDefault }

    fun setWorkflowDefault(workflowDefault: WorkflowConfigDefault?) =
        apply { this.workflowDefault = workflowDefault }

    fun setEventListener(eventListener: EventListenerConfig?) =
        apply { this.eventListener = eventListener }

    fun build() = WorkerConfig(
        name,
        shutdownGracePeriodInSeconds,
        transport,
        pulsar,
        storage,
        logs,
        workflows,
        services,
        serviceDefault,
        workflowDefault,
        eventListener,
    )
  }
}
