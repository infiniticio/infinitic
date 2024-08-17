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
  val serviceTagEngines = mutableMapOf<ServiceName, RegisteredServiceTagEngine>()
  val serviceExecutors = mutableMapOf<ServiceName, RegisteredServiceExecutor>()
  val serviceEventListeners = mutableMapOf<ServiceName, RegisteredEventListener>()
  val serviceEventLoggers = mutableMapOf<ServiceName, RegisteredEventLogger>()

  val workflowTagEngines = mutableMapOf<WorkflowName, RegisteredWorkflowTagEngine>()
  val workflowStateEngines = mutableMapOf<WorkflowName, RegisteredWorkflowStateEngine>()
  val workflowExecutors = mutableMapOf<WorkflowName, RegisteredWorkflowExecutor>()
  val workflowEventListeners = mutableMapOf<WorkflowName, RegisteredEventListener>()
  val workflowEventLoggers = mutableMapOf<WorkflowName, RegisteredEventLogger>()

  fun getRegisteredServiceTagEngine(serviceName: ServiceName) =
      serviceTagEngines[serviceName]

  fun getRegisteredServiceExecutor(serviceName: ServiceName) =
      serviceExecutors[serviceName]

  fun getRegisteredServiceEventListener(serviceName: ServiceName) =
      serviceEventListeners[serviceName]

  fun getRegisteredServiceEventLogger(serviceName: ServiceName) =
      serviceEventLoggers[serviceName]

  fun getRegisteredWorkflowTagEngine(workflowName: WorkflowName) =
      workflowTagEngines[workflowName]

  fun getRegisteredWorkflowStateEngine(workflowName: WorkflowName) =
      workflowStateEngines[workflowName]

  fun getRegisteredWorkflowExecutor(workflowName: WorkflowName) =
      workflowExecutors[workflowName]

  fun getRegisteredWorkflowEventListener(workflowName: WorkflowName) =
      workflowEventListeners[workflowName]

  fun getRegisteredWorkflowEventLogger(workflowName: WorkflowName) =
      workflowEventLoggers[workflowName]

  @TestOnly
  fun flush() {
    serviceTagEngines.values.forEach { it.storage.flush() }
    workflowTagEngines.values.forEach { it.storage.flush() }
    workflowStateEngines.values.forEach { it.storage.flush() }
  }
}
