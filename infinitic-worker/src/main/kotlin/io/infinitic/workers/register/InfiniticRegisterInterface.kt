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

import io.infinitic.common.workers.registry.ServiceFactory
import io.infinitic.common.workers.registry.WorkerRegistry
import io.infinitic.common.workers.registry.WorkflowClassList
import io.infinitic.events.config.EventListener
import io.infinitic.tasks.WithRetry
import io.infinitic.tasks.WithTimeout
import io.infinitic.tasks.tag.config.TaskTag
import io.infinitic.workflows.Workflow
import io.infinitic.workflows.WorkflowCheckMode
import io.infinitic.workflows.engine.config.WorkflowEngine
import io.infinitic.workflows.tag.config.WorkflowTag

interface InfiniticRegisterInterface : AutoCloseable {

  val registry: WorkerRegistry

  companion object {
    /**
     * Note: Final default values for withRetry, withTimeout and workflow check mode
     * are in TaskExecutors as they can be defined through annotations as well
     */
    const val DEFAULT_CONCURRENCY = 1
    val DEFAULT_EVENT_LISTENER = null
    val DEFAULT_TASK_TAG = TaskTag().apply { isDefault = true }
    val DEFAULT_WORKFLOW_ENGINE = WorkflowEngine().apply { isDefault = true }
    val DEFAULT_WORKFLOW_TAG = WorkflowTag().apply { isDefault = true }
  }

  /** Register service */
  @Suppress("OVERLOADS_INTERFACE")
  @JvmOverloads
  fun registerService(
    name: String,
    factory: ServiceFactory,
    concurrency: Int = DEFAULT_CONCURRENCY,
    timeout: WithTimeout? = null,
    retry: WithRetry? = null,
    tagEngine: TaskTag? = DEFAULT_TASK_TAG,
    eventListener: EventListener? = DEFAULT_EVENT_LISTENER
  )

  /** Register workflow */
  @Suppress("OVERLOADS_INTERFACE")
  @JvmOverloads
  fun registerWorkflow(
    name: String,
    classes: WorkflowClassList,
    concurrency: Int = DEFAULT_CONCURRENCY,
    timeout: WithTimeout? = null,
    retry: WithRetry? = null,
    checkMode: WorkflowCheckMode? = null,
    engine: WorkflowEngine? = DEFAULT_WORKFLOW_ENGINE,
    tagEngine: WorkflowTag? = DEFAULT_WORKFLOW_TAG
  )

  @Suppress("OVERLOADS_INTERFACE")
  @JvmOverloads
  fun registerWorkflow(
    name: String,
    `class`: Class<out Workflow>,
    concurrency: Int = DEFAULT_CONCURRENCY,
    timeout: WithTimeout? = null,
    retry: WithRetry? = null,
    checkMode: WorkflowCheckMode? = null,
    engine: WorkflowEngine? = DEFAULT_WORKFLOW_ENGINE,
    tagEngine: WorkflowTag? = DEFAULT_WORKFLOW_TAG
  ) = registerWorkflow(
      name,
      listOf(`class`),
      concurrency,
      timeout,
      retry,
      checkMode,
      engine,
      tagEngine,
  )
}
