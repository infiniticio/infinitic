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
import io.infinitic.common.config.loadConfigFromYaml
import io.infinitic.events.config.EventListener
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
  override val workflowDefault: WorkflowDefault = WorkflowDefault(),

  /** Default event listener configuration */
  override val eventListener: EventListener? = null

) : WorkerConfigInterface {

  init {
    // check default service retry Policy
    serviceDefault.retry?.check()

    // apply default, if not set
    services.map { s ->
      // if service.concurrency, is not defined
      // then use serviceDefault.concurrency
      // then DEFAULT_CONCURRENCY
      s.concurrency = s.concurrency ?: serviceDefault.concurrency ?: DEFAULT_CONCURRENCY
      // if service.retry, is not defined
      // then use serviceDefault.retry
      s.retry = (s.retry ?: serviceDefault.retry)?.also { it.check() }
      // if service.timeoutInSeconds, is not defined
      // then use serviceDefault.timeoutInSeconds
      s.timeoutInSeconds = s.timeoutInSeconds ?: serviceDefault.timeoutInSeconds

      s.tagEngine?.apply {
        // if storage is not defined,
        // then use default tag engine storage
        // else use default storage
        storage = storage ?: serviceDefault.tagEngine?.storage ?: storage
        // if cache is not defined,
        // then use default tag engine cache
        // else use default cache
        cache = cache ?: serviceDefault.tagEngine?.cache ?: cache
        // if concurrency is not defined,
        // then use default tag engine concurrency
        // else use service concurrency
        concurrency = concurrency ?: serviceDefault.tagEngine?.concurrency ?: s.concurrency
      }

      s.eventListener = (s.eventListener ?: eventListener)?.apply {
        // if concurrency is not defined,
        // then use default event listener concurrency
        // else use service concurrency
        concurrency = concurrency ?: eventListener?.concurrency ?: s.concurrency
        // if class is not defined
        // then use default event listener class
        `class` = `class` ?: eventListener?.`class`
        // class must be defined eventually
        require(`class` != null) { "No `class` is defined for the event listener of service '${s.name}'" }
      }
    }

    // check default service retry Policy
    workflowDefault.retry?.check()

    // apply default, if not set
    workflows.map { w ->
      // if workflow.concurrency, is not defined
      // then use workflowDefault.concurrency
      // then DEFAULT_CONCURRENCY
      w.concurrency = w.concurrency ?: workflowDefault.concurrency ?: DEFAULT_CONCURRENCY
      // if workflow.retry, is not defined
      // then use workflowDefault.retry
      w.retry = (w.retry ?: workflowDefault.retry)?.also { it.check() }
      // if workflow.timeoutInSeconds, is not defined
      // then use workflowDefault.timeoutInSeconds
      w.timeoutInSeconds = w.timeoutInSeconds ?: workflowDefault.timeoutInSeconds
      // if workflow.checkMode, is not defined
      // then use workflowDefault.checkMode
      w.checkMode = w.checkMode ?: workflowDefault.checkMode

      w.tagEngine?.apply {
        // if storage is not defined,
        // then use default workflow tag engine storage
        // else use default storage
        storage = storage ?: workflowDefault.tagEngine?.storage ?: storage
        // if cache is not defined,
        // then use default workflow tag engine cache
        // else use default cache
        cache = cache ?: workflowDefault.tagEngine?.cache ?: cache
        // if concurrency is not defined,
        // then use default workflow tag engine concurrency
        // else use workflow concurrency
        concurrency = concurrency ?: workflowDefault.tagEngine?.concurrency ?: w.concurrency
      }

      w.workflowEngine?.apply {
        // if storage is not defined,
        // then use default workflow workflowEngine engine storage
        // else use default storage
        storage = storage ?: workflowDefault.workflowEngine?.storage ?: storage
        // if cache is not defined,
        // then use default workflow workflowEngine engine cache
        // else use default cache
        cache = cache ?: workflowDefault.workflowEngine?.cache ?: cache
        // if concurrency is not defined,
        // then use default workflow workflowEngine engine concurrency
        // else use workflow concurrency
        concurrency = concurrency ?: workflowDefault.workflowEngine?.concurrency ?: w.concurrency
      }

      // contrary to tagEngine/workflowEngine,
      // you can not set null for the eventListener if a default event listener is provided
      w.eventListener = (w.eventListener ?: eventListener)?.apply {
        // if concurrency is not defined,
        // then use default event listener concurrency
        // else use service concurrency
        concurrency = concurrency ?: eventListener?.concurrency ?: w.concurrency
        // if class is not defined
        // then use default event listener class
        `class` = `class` ?: eventListener?.`class`
        // class must be defined eventually
        require(`class` != null) { "No `class` is defined for the event listener of workflow '${w.name}'" }
      }
    }
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
