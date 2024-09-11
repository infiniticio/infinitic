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
package io.infinitic.common.registry

import io.infinitic.common.tasks.data.ServiceName
import io.infinitic.common.workflows.data.workflowTasks.WorkflowTaskParameters
import io.infinitic.common.workflows.data.workflows.WorkflowName
import io.infinitic.tasks.WithRetry
import io.infinitic.tasks.WithTimeout
import io.infinitic.workflows.Workflow
import io.infinitic.workflows.WorkflowCheckMode

interface ExecutorRegistryInterface {
  fun getServiceExecutorInstance(serviceName: ServiceName): Any
  fun getServiceExecutorWithTimeout(serviceName: ServiceName): WithTimeout?
  fun getServiceExecutorWithRetry(serviceName: ServiceName): WithRetry?

  fun getWorkflowExecutorInstance(workflowTaskParameters: WorkflowTaskParameters): Workflow
  fun getWorkflowExecutorWithTimeout(workflowName: WorkflowName): WithTimeout?
  fun getWorkflowExecutorWithRetry(workflowName: WorkflowName): WithRetry?
  fun getWorkflowExecutorCheckMode(workflowName: WorkflowName): WorkflowCheckMode?
}
