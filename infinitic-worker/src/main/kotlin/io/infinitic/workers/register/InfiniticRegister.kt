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

import io.infinitic.cache.Cache
import io.infinitic.cloudEvents.CloudEventListener
import io.infinitic.common.workers.registry.ServiceFactory
import io.infinitic.common.workers.registry.WorkerRegistry
import io.infinitic.common.workers.registry.WorkflowFactories
import io.infinitic.events.config.EventListener
import io.infinitic.storage.Storage
import io.infinitic.tasks.WithRetry
import io.infinitic.tasks.WithTimeout
import io.infinitic.workers.register.config.ServiceDefault
import io.infinitic.workers.register.config.UNDEFINED_WITH_RETRY
import io.infinitic.workers.register.config.UNDEFINED_WITH_TIMEOUT
import io.infinitic.workers.register.config.WorkflowDefault
import io.infinitic.workflows.Workflow
import io.infinitic.workflows.WorkflowCheckMode

interface InfiniticRegister : AutoCloseable {

  val registry: WorkerRegistry

  /**
   * Default value of Storage
   */
  var defaultStorage: Storage

  /**
   * Default value of Cache
   */
  var defaultCache: Cache

  /**
   * Service default values
   */
  var serviceDefault: ServiceDefault

  /**
   * Workflow default values
   */
  var workflowDefault: WorkflowDefault

  /**
   * Default value of EventListener
   */
  var defaultEventListener: EventListener?

  /** Register service tag engine */
  @Suppress("OVERLOADS_INTERFACE")
  @JvmOverloads
  fun registerServiceTagEngine(
    serviceName: String,
    concurrency: Int? = null,
    storage: Storage? = null,
    cache: Cache? = null
  )

  /** Register service */
  @Suppress("OVERLOADS_INTERFACE")
  @JvmOverloads
  fun registerServiceExecutor(
    serviceName: String,
    serviceFactory: ServiceFactory,
    concurrency: Int? = null,
    withTimeout: WithTimeout? = UNDEFINED_WITH_TIMEOUT,
    withRetry: WithRetry? = UNDEFINED_WITH_RETRY
  )


  /** Register service event listener */
  @Suppress("OVERLOADS_INTERFACE")
  @JvmOverloads
  fun registerServiceEventListener(
    serviceName: String,
    concurrency: Int? = null,
    eventListener: CloudEventListener? = null,
    subscriptionName: String? = null,
  )

  /** Register workflow */
  @Suppress("OVERLOADS_INTERFACE")
  @JvmOverloads
  fun registerWorkflowExecutor(
    workflowName: String,
    factories: WorkflowFactories,
    concurrency: Int? = null,
    withTimeout: WithTimeout? = UNDEFINED_WITH_TIMEOUT,
    withRetry: WithRetry? = UNDEFINED_WITH_RETRY,
    checkMode: WorkflowCheckMode? = null,
  )

  @Suppress("OVERLOADS_INTERFACE")
  @JvmOverloads
  fun registerWorkflowExecutor(
    workflowName: String,
    factory: () -> Workflow,
    concurrency: Int? = null,
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
    concurrency: Int? = null,
    storage: Storage? = null,
    cache: Cache? = null
  )

  /** Register workflow tag engine */
  @Suppress("OVERLOADS_INTERFACE")
  @JvmOverloads
  fun registerWorkflowTagEngine(
    workflowName: String,
    concurrency: Int? = null,
    storage: Storage? = null,
    cache: Cache? = null
  )

  /** Register workflow event listener */
  @Suppress("OVERLOADS_INTERFACE")
  @JvmOverloads
  fun registerWorkflowEventListener(
    workflowName: String,
    concurrency: Int? = null,
    eventListener: CloudEventListener? = null,
    subscriptionName: String? = null,
  )
}
