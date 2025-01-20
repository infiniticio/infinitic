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
package io.infinitic.common.workflows.tags.storage

import io.infinitic.common.storage.Flushable
import io.infinitic.common.workflows.data.workflows.WorkflowId
import io.infinitic.common.workflows.data.workflows.WorkflowName
import io.infinitic.common.workflows.data.workflows.WorkflowTag

/**
 * TagStateStorage implementations are responsible for storing the different state objects used by
 * the engine.
 *
 * No assumptions are made on whether the storage should be persistent or not, nor how the data
 * should be transformed before being stored. These details are left to the different
 * implementations.
 */
interface WorkflowTagStorage : Flushable {
  suspend fun getWorkflowIds(tag: WorkflowTag, workflowName: WorkflowName): Set<WorkflowId>

  suspend fun addWorkflowId(tag: WorkflowTag, workflowName: WorkflowName, workflowId: WorkflowId)

  suspend fun removeWorkflowId(tag: WorkflowTag, workflowName: WorkflowName, workflowId: WorkflowId)

  suspend fun getWorkflowIds(tagAndNames: Set<Pair<WorkflowTag, WorkflowName>>): Map<Pair<WorkflowTag, WorkflowName>, Set<WorkflowId>>

  suspend fun updateWorkflowIds(
    add: Map<Pair<WorkflowTag, WorkflowName>, Set<WorkflowId>>,
    remove: Map<Pair<WorkflowTag, WorkflowName>, Set<WorkflowId>>
  )
}
