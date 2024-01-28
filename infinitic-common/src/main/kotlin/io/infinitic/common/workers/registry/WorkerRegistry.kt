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
package io.infinitic.common.workers.registry

import io.infinitic.common.tasks.data.ServiceName
import io.infinitic.common.workflows.data.workflows.WorkflowName
import org.jetbrains.annotations.TestOnly

class WorkerRegistry {
  val serviceExecutors = mutableMapOf<ServiceName, RegisteredServiceExecutor>()
  val serviceTags = mutableMapOf<ServiceName, RegisteredServiceTag>()
  val serviceListeners = mutableMapOf<ServiceName, RegisteredEventListener>()

  val workflowEngines = mutableMapOf<WorkflowName, RegisteredWorkflowEngine>()
  val workflowExecutors = mutableMapOf<WorkflowName, RegisteredWorkflowExecutor>()
  val workflowTags = mutableMapOf<WorkflowName, RegisteredWorkflowTag>()
  val workflowListeners = mutableMapOf<WorkflowName, RegisteredEventListener>()

  fun getRegisteredService(serviceName: ServiceName) =
      serviceExecutors[serviceName]

  fun getRegisteredServiceTag(serviceName: ServiceName) =
      serviceTags[serviceName]

  fun getRegisteredWorkflow(workflowName: WorkflowName) =
      workflowExecutors[workflowName]

  fun getRegisteredWorkflowEngine(workflowName: WorkflowName) =
      workflowEngines[workflowName]

  fun getRegisteredWorkflowTag(workflowName: WorkflowName) =
      workflowTags[workflowName]

  fun getRegisteredServiceEventListener(serviceName: ServiceName) =
      serviceListeners[serviceName]

  fun getRegisteredWorkflowEventListener(workflowName: WorkflowName) =
      workflowListeners[workflowName]

  @TestOnly
  fun flush() {
    serviceTags.values.forEach { it.storage.flush() }
    workflowTags.values.forEach { it.storage.flush() }
    workflowEngines.values.forEach { it.storage.flush() }
  }
}
