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

import io.infinitic.cache.config.Cache
import io.infinitic.cloudEvents.CloudEventListener
import io.infinitic.common.workers.registry.ServiceFactory
import io.infinitic.common.workers.registry.WorkerRegistry
import io.infinitic.common.workers.registry.WorkflowClassList
import io.infinitic.storage.config.Storage
import io.infinitic.tasks.WithRetry
import io.infinitic.tasks.WithTimeout
import io.infinitic.workflows.Workflow
import io.infinitic.workflows.WorkflowCheckMode

interface InfiniticRegister : AutoCloseable {

  val registry: WorkerRegistry

  /** Register service tag engine */
  fun registerServiceTagEngine(
    serviceName: String,
    concurrency: Int,
    storage: Storage,
    cache: Cache
  )

  /** Register service */
  @Suppress("OVERLOADS_INTERFACE")
  @JvmOverloads
  fun registerServiceExecutor(
    serviceName: String,
    serviceFactory: ServiceFactory,
    concurrency: Int = DEFAULT_CONCURRENCY,
    withTimeout: WithTimeout? = null,
    withRetry: WithRetry? = null
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

  /** Register workflow */
  @Suppress("OVERLOADS_INTERFACE")
  @JvmOverloads
  fun registerWorkflowExecutor(
    workflowName: String,
    classes: WorkflowClassList,
    concurrency: Int = DEFAULT_CONCURRENCY,
    withTimeout: WithTimeout? = null,
    withRetry: WithRetry? = null,
    checkMode: WorkflowCheckMode? = null,
  )

  @Suppress("OVERLOADS_INTERFACE")
  @JvmOverloads
  fun registerWorkflowExecutor(
    workflowName: String,
    `class`: Class<out Workflow>,
    concurrency: Int = DEFAULT_CONCURRENCY,
    withTimeout: WithTimeout? = null,
    withRetry: WithRetry? = null,
    checkMode: WorkflowCheckMode? = null,
  ) = registerWorkflowExecutor(
      workflowName,
      listOf(`class`),
      concurrency,
      withTimeout,
      withRetry,
      checkMode,
  )

  /** Register workflow state engine */
  fun registerWorkflowStateEngine(
    workflowName: String,
    concurrency: Int,
    storage: Storage,
    cache: Cache
  )

  /** Register workflow tag engine */
  fun registerWorkflowTagEngine(
    workflowName: String,
    concurrency: Int,
    storage: Storage,
    cache: Cache
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

  companion object {
    /**
     * Note: Final default values for withRetry, withTimeout and workflow check mode
     * are in TaskExecutors as they can be defined through annotations as well
     */
    const val DEFAULT_CONCURRENCY = 1
  }
}
