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

import io.infinitic.cache.config.Cache
import io.infinitic.common.config.loadConfigFromFile
import io.infinitic.common.config.loadConfigFromResource
import io.infinitic.pulsar.config.Pulsar
import io.infinitic.storage.config.Storage
import io.infinitic.transport.config.Transport
import io.infinitic.workers.register.InfiniticRegisterInterface.Companion.DEFAULT_CONCURRENCY
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
  override val storage: Storage = Storage(),

  /** Default cache */
  override var cache: Cache = Cache(),

  /** Workflows configuration */
  override val workflows: List<Workflow> = listOf(),

  /** Services configuration */
  override val services: List<Service> = listOf(),

  /** Default service configuration */
  override val serviceDefault: ServiceDefault = ServiceDefault(),

  /** Default workflow configuration */
  override val workflowDefault: WorkflowDefault = WorkflowDefault()

) : WorkerConfigInterface {

  init {
    // check default service retry Policy
    serviceDefault.retry?.check()

    // apply default, if not set
    services.map { s ->
      s.concurrency =
          s.concurrency ?: serviceDefault.concurrency ?: DEFAULT_CONCURRENCY
      s.retry =
          (s.retry ?: serviceDefault.retry)?.also { it.check() }
      s.timeoutInSeconds =
          s.timeoutInSeconds ?: serviceDefault.timeoutInSeconds

      // tagEngine default is superseded by service default if any
      if (s.tagEngine != null && s.tagEngine!!.isDefault) {
        serviceDefault.tagEngine?.let {
          s.tagEngine = it.apply {
            concurrency = concurrency ?: s.concurrency
          }
        }
      }

      s.tagEngine?.let {
        it.storage =
            it.storage ?: serviceDefault.tagEngine?.storage ?: storage
        it.cache =
            it.cache ?: serviceDefault.tagEngine?.cache ?: cache
        it.concurrency =
            it.concurrency ?: serviceDefault.tagEngine?.concurrency ?: DEFAULT_CONCURRENCY
      }
    }

    // check default service retry Policy
    workflowDefault.retry?.check()

    // apply default, if not set
    workflows.map { w ->
      w.concurrency =
          w.concurrency ?: workflowDefault.concurrency ?: DEFAULT_CONCURRENCY
      w.retry =
          (w.retry ?: workflowDefault.retry)?.also { it.check() }
      w.timeoutInSeconds =
          w.timeoutInSeconds ?: workflowDefault.timeoutInSeconds
      w.checkMode =
          w.checkMode ?: workflowDefault.checkMode

      // tagEngine default is superseded by workflow default if any
      if (w.tagEngine != null && w.tagEngine!!.isDefault) {
        workflowDefault.tagEngine?.let {
          w.tagEngine = it.apply {
            concurrency = concurrency ?: w.concurrency
          }
        }
      }

      w.tagEngine?.let {
        it.storage =
            it.storage ?: workflowDefault.tagEngine?.storage ?: storage
        it.cache =
            it.cache ?: workflowDefault.tagEngine?.cache ?: cache
        it.concurrency =
            it.concurrency ?: workflowDefault.tagEngine?.concurrency ?: DEFAULT_CONCURRENCY
      }

      // workflowEngine default is superseded by workflow default if any
      if (w.workflowEngine != null && w.workflowEngine!!.isDefault) {
        workflowDefault.workflowEngine?.let {
          w.workflowEngine = it.apply {
            concurrency?.let { concurrency = w.concurrency }
          }
        }
      }

      w.workflowEngine?.let {
        it.storage =
            it.storage ?: workflowDefault.workflowEngine?.storage ?: storage
        it.cache =
            it.cache ?: workflowDefault.workflowEngine?.cache ?: cache
        it.concurrency =
            it.concurrency ?: workflowDefault.workflowEngine?.concurrency ?: DEFAULT_CONCURRENCY
      }
    }
  }

  companion object {
    /** Create ClientConfig from file in file system */
    @JvmStatic
    fun fromFile(vararg files: String): WorkerConfig =
        loadConfigFromFile(files.toList())

    /** Create ClientConfig from file in resources directory */
    @JvmStatic
    fun fromResource(vararg resources: String): WorkerConfig =
        loadConfigFromResource(resources.toList())
  }
}
