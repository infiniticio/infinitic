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
import io.infinitic.events.config.EventListener
import io.infinitic.pulsar.config.Pulsar
import io.infinitic.storage.Storage
import io.infinitic.transport.config.Transport
import io.infinitic.workers.register.config.Service
import io.infinitic.workers.register.config.ServiceDefault
import io.infinitic.workers.register.config.Workflow
import io.infinitic.workers.register.config.WorkflowDefault

data class WorkerConfig @JvmOverloads constructor(
  /** Worker name */
  override val name: String? = null,

  /** Worker name */
  override val shutdownGracePeriodInSeconds: Double = 30.0,

  /** Transport configuration */
  override val transport: Transport = Transport.pulsar,

  /** Pulsar configuration */
  override val pulsar: Pulsar? = null,

  /** Default storage */
  override val storage: Storage? = null,

  /** Workflows configuration */
  override val workflows: List<Workflow> = listOf(),

  /** Services configuration */
  override val services: List<Service> = listOf(),

  /** Default service configuration */
  override val serviceDefault: ServiceDefault? = null,

  /** Default workflow configuration */
  override val workflowDefault: WorkflowDefault? = null,

  /** Default event listener configuration */
  override val eventListener: EventListener? = null

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
  }
}
