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
package io.infinitic.workers.register

import io.infinitic.cloudEvents.CloudEventListener
import io.infinitic.common.workers.registry.ServiceFactory
import io.infinitic.common.workers.registry.WorkerRegistry
import io.infinitic.common.workers.registry.WorkflowFactories
import io.infinitic.events.config.EventListenerConfig
import io.infinitic.storage.config.StorageConfig
import io.infinitic.tasks.WithRetry
import io.infinitic.tasks.WithTimeout
import io.infinitic.workers.register.config.LogsConfig
import io.infinitic.workers.register.config.ServiceConfigDefault
import io.infinitic.workers.register.config.UNDEFINED_WITH_RETRY
import io.infinitic.workers.register.config.UNDEFINED_WITH_TIMEOUT
import io.infinitic.workers.register.config.WorkflowConfigDefault
import io.infinitic.workflows.Workflow
import io.infinitic.workflows.WorkflowCheckMode

@Suppress("unused")
interface InfiniticRegister : AutoCloseable {

  val registry: WorkerRegistry

  /**
   * Logs configuration
   */
  var logsConfig: LogsConfig

  /**
   * Default value of Storage
   */
  var defaultStorage: StorageConfig

  /**
   * Default value of EventListener
   */
  var defaultEventListener: EventListenerConfig?

  /**
   * Service default values
   */
  var serviceDefault: ServiceConfigDefault

  /**
   * Workflow default values
   */
  var workflowDefault: WorkflowConfigDefault

  /**
   *
   * Register Services
   *
   */

  /** Register service tag engine */
  @Suppress("OVERLOADS_INTERFACE")
  @JvmOverloads
  fun registerServiceTagEngine(
    serviceName: String,
    concurrency: Int,
    storageConfig: StorageConfig? = null
  )

  /** Register service */
  @Suppress("OVERLOADS_INTERFACE")
  @JvmOverloads
  fun registerServiceExecutor(
    serviceName: String,
    serviceFactory: ServiceFactory,
    concurrency: Int,
    withTimeout: WithTimeout? = UNDEFINED_WITH_TIMEOUT,
    withRetry: WithRetry? = UNDEFINED_WITH_RETRY
  )


  /** Register service event listener */
  @Suppress("OVERLOADS_INTERFACE")
  @JvmOverloads
  fun registerServiceEventListener(
    serviceName: String,
    concurrency: Int,
    eventListener: CloudEventListener,
    subscriptionName: String? = null,
  )

  /**
   *
   * Register workflows
   *
   */

  /** Register workflow tag engine */
  @Suppress("OVERLOADS_INTERFACE")
  @JvmOverloads
  fun registerWorkflowTagEngine(
    workflowName: String,
    concurrency: Int,
    storageConfig: StorageConfig? = null
  )

  /** Register workflow executor  */
  @Suppress("OVERLOADS_INTERFACE")
  @JvmOverloads
  fun registerWorkflowExecutor(
    workflowName: String,
    factories: WorkflowFactories,
    concurrency: Int,
    withTimeout: WithTimeout?,
    withRetry: WithRetry?,
    checkMode: WorkflowCheckMode? = null,
  )

  /** Register workflow executor  */
  @Suppress("OVERLOADS_INTERFACE")
  @JvmOverloads
  fun registerWorkflowExecutor(
    workflowName: String,
    factory: () -> Workflow,
    concurrency: Int,
    withTimeout: WithTimeout? = UNDEFINED_WITH_TIMEOUT,
    withRetry: WithRetry? = UNDEFINED_WITH_RETRY,
    checkMode: WorkflowCheckMode? = null,
  ) = registerWorkflowExecutor(
      workflowName,
      listOf(factory),
      concurrency,
      withTimeout,
      withRetry,
      checkMode,
  )

  /** Register workflow state engine */
  @Suppress("OVERLOADS_INTERFACE")
  @JvmOverloads
  fun registerWorkflowStateEngine(
    workflowName: String,
    concurrency: Int,
    storageConfig: StorageConfig? = null
  )

  /** Register workflow event listener */
  @Suppress("OVERLOADS_INTERFACE")
  @JvmOverloads
  fun registerWorkflowEventListener(
    workflowName: String,
    concurrency: Int,
    eventListener: CloudEventListener,
    subscriptionName: String? = null,
  )
}
