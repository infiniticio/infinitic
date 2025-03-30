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
package io.infinitic.common.workflows.engine.storage

import io.infinitic.common.storage.Flushable
import io.infinitic.common.workflows.data.workflows.WorkflowId
import io.infinitic.common.workflows.engine.state.WorkflowState

interface WorkflowStateStorage : Flushable {
  suspend fun getState(workflowId: WorkflowId): WorkflowState?

  suspend fun putState(workflowId: WorkflowId, workflowState: WorkflowState?)

  suspend fun getStates(workflowIds: List<WorkflowId>): Map<WorkflowId, WorkflowState?>

  suspend fun putStates(workflowStates: Map<WorkflowId, WorkflowState?>)

  /**
   * Updates the state only if the current version matches the expected version.
   * @return true if the update was successful, false if the version didn't match
   */
  suspend fun putStateWithVersion(
    workflowId: WorkflowId,
    workflowState: WorkflowState?,
    expectedVersion: Long
  ): Boolean

  /**
   * Gets both state and version in a single call for efficiency
   * @return Pair of state and version, where version is 0 if state is null
   */
  suspend fun getStateAndVersion(workflowId: WorkflowId): Pair<WorkflowState?, Long>
}
